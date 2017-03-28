#define PJON_MAX_PACKETS 10
#define SWBB_MAX_ATTEMPTS 50
#define MPU_INTERRUPT_PIN digitalPinToInterrupt(2)

#include <PJON.h>
#include <PJONSlave.h>
#include "I2Cdev.h"
#include "MPU6050_6Axis_MotionApps20.h"

PJONSlave<SoftwareBitBang> bus;
MPU6050 mpu;

bool dmpReady = false;
uint8_t mpuIntStatus;
uint8_t devStatus;
uint16_t packetSize;
uint16_t fifoCount;
uint8_t fifoBuffer[64];
char teapotPacket[11] = { '$', 0xFF, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

volatile bool mpuInterrupt = false;
void dmpDataReady() {
  mpuInterrupt = true;
}

void blink_led(uint8_t interval) {
  digitalWrite(LED_BUILTIN, HIGH);
  delay(interval);
  digitalWrite(LED_BUILTIN, LOW);
  delay(interval);
}

void blink_long(uint8_t times = 2) {
  for (uint8_t i = 0; i < times; i++) {
    blink_led(1500);
  }
}

void blink_short(uint8_t times = 2) {
  for (uint8_t i = 0; i < times; i++) {
    blink_led(250);
  }
}

void receiver_function(uint8_t *payload, uint16_t length, const PJON_Packet_Info &packet_info) {
  if ((char)payload[0] == 'I') {
    blink_led(500);
  }
}

void error_handler(uint8_t code, uint8_t data) {
  if(code == PJON_CONNECTION_LOST) {
    blink_short();
  }
  if(code == PJON_PACKETS_BUFFER_FULL) {
    blink_short(4);
  }
}

void setup() {
  I2c.begin();
  I2c.pullup(0);
  I2c.timeOut(5000);

  pinMode(LED_BUILTIN, OUTPUT);
  pinMode(MPU_INTERRUPT_PIN, INPUT);

  mpu.initialize();
  if (mpu.testConnection()) {
    devStatus = mpu.dmpInitialize();
    if (devStatus == 0) {
      blink_long(6);
      mpu.setDMPEnabled(true);
      attachInterrupt(MPU_INTERRUPT_PIN, dmpDataReady, RISING);
      mpuIntStatus = mpu.getIntStatus();
      dmpReady = true;
      packetSize = mpu.dmpGetFIFOPacketSize();

      mpu.setSleepEnabled(false);
      blink_short();
    } else {
      blink_long(10);
    }
  } else {
    blink_long(8);
  }

  bus.strategy.set_pin(12);
  bus.set_receiver(receiver_function);
  bus.set_error(error_handler);
  bus.begin();
}

void loop() {
  if (!dmpReady) {
    return;
  }

  bus.send(PJON_MASTER_ID, teapotPacket, sizeof(teapotPacket));
  bus.receive(50000);
  bus.update();

  teapotPacket[10] = 0;

  //Serial.println(F("Waiting on INT..."));
  while (!mpuInterrupt && fifoCount < packetSize);

  mpuInterrupt = false;
  //Serial.println(F("getIntStatus()"));
  mpuIntStatus = mpu.getIntStatus();

  fifoCount = mpu.getFIFOCount();

  if ((mpuIntStatus & 0x10) || fifoCount == 1024) {
    //Serial.println(F("resetFIFO()"));
    mpu.resetFIFO();
  } else if (mpuIntStatus & 0x02) {
    //Serial.println(F("getFIFOCount()"));
    while (fifoCount < packetSize) fifoCount = mpu.getFIFOCount();

    //Serial.println(F("getFIFOBytes()"));
    mpu.getFIFOBytes(fifoBuffer, packetSize);

    fifoCount -= packetSize;

    teapotPacket[1] = bus.device_id();
    teapotPacket[2] = fifoBuffer[0];
    teapotPacket[3] = fifoBuffer[1];
    teapotPacket[4] = fifoBuffer[4];
    teapotPacket[5] = fifoBuffer[5];
    teapotPacket[6] = fifoBuffer[8];
    teapotPacket[7] = fifoBuffer[9];
    teapotPacket[8] = fifoBuffer[12];
    teapotPacket[9] = fifoBuffer[13];
    teapotPacket[10] = 1;
  }
  //delay(millis() - t);
  //Serial.println(F(""));
}
