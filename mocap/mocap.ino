#include "Wire.h"
#include "I2Cdev.h"
#include "MPU6050_6Axis_MotionApps20.h"

#define TCAADDR 0x70
#define NUM_MPUS 2

struct MPU {
  MPU6050 mpu;            // pointer to the device
  
  bool dmpReady = false;  // set true if DMP init was successful
  uint8_t mpuIntStatus;   // holds actual interrupt status byte from MPU
  uint8_t devStatus;      // return status after each device operation (0 = success, !0 = error)
  uint16_t packetSize;    // expected DMP packet size (default is 42 bytes)
  uint16_t fifoCount;     // count of all bytes currently in FIFO
  uint8_t fifoBuffer[64]; // FIFO storage buffer
  uint8_t teapotPacket[14] = { '$', 0x02, 0,0, 0,0, 0,0, 0,0, 0x00, 0x00, '\r', '\n' };
};

MPU mpus[NUM_MPUS];
byte mpuIndex[] = { 2, 3 };

/** 
 *  Selects the given device in the multiplexer.
 */
void tcaselect(uint8_t i) {
  if (i > 7) return;
 
  Wire.beginTransmission(TCAADDR);
  Wire.write(1 << i);
  Wire.endTransmission();  
}

void setup() {
    // join I2C bus (I2Cdev library doesn't do this automatically)
    Wire.begin();

    // initialize serial communication
    // (115200 chosen because it is required for Teapot Demo output, but it's
    // really up to you depending on your project)
    Serial.begin(115200);
    while (!Serial); // wait for Leonardo enumeration, others continue immediately

    // NOTE: 8MHz or slower host processors, like the Teensy @ 3.3v or Ardunio
    // Pro Mini running at 3.3v, cannot handle this baud rate reliably due to
    // the baud timing being too misaligned with processor ticks. You must use
    // 38400 or slower in these cases, or use some kind of external separate
    // crystal solution for the UART timer.

    // initialize devices
    Serial.println(F("Initializing I2C devices..."));
    for (byte i = 0; i < NUM_MPUS; i++) {
      tcaselect(mpuIndex[i]);

      MPU6050 mpu = mpus[i].mpu;
      
      mpu.initialize();

      // verify connection
      Serial.print(F("Testing device '")); Serial.print(mpuIndex[i]); Serial.println(F("' connection..."));
      Serial.println(mpu.testConnection() ? F("MPU6050 connection successful") : F("MPU6050 connection failed"));

      // load and configure the DMP
      Serial.println(F("Initializing DMP..."));
      mpus[i].devStatus = mpu.dmpInitialize();
    
      // make sure it worked (returns 0 if so)
      if (mpus[i].devStatus == 0) {
        // turn on the DMP, now that it's ready
        Serial.println(F("Enabling DMP..."));
        mpu.setDMPEnabled(true);

        // enable Arduino interrupt detection
        //Serial.println(F("Enabling interrupt detection (Arduino external interrupt 0)..."));
        //attachInterrupt(digitalPinToInterrupt(7), dmpDataReady, RISING);
        mpus[i].mpuIntStatus = mpu.getIntStatus();

        // set our DMP Ready flag so the main loop() function knows it's okay to use it
        Serial.println(F("DMP ready!"));
        mpus[i].dmpReady = true;

        // get expected DMP packet size for later comparison
        mpus[i].packetSize = mpu.dmpGetFIFOPacketSize();
      } else {
          // ERROR!
          // 1 = initial memory load failed
          // 2 = DMP configuration updates failed
          // (if it's going to break, usually the code will be 1)
          Serial.print(F("DMP Initialization failed (code "));
          Serial.print(mpus[i].devStatus);
          Serial.println(F(")"));
      }
  }

  // wait for ready
  Serial.println(F("\nSend any character to begin DMP programming and demo: "));
  while (Serial.available() && Serial.read()); // empty buffer
  while (!Serial.available());                 // wait for data
  while (Serial.available() && Serial.read()); // empty buffer again
}

void processMPU(MPU mpuToProcess, byte mpuID) {
    // if programming failed, don't try to do anything
    if (!mpuToProcess.dmpReady) return;

    // get INT_STATUS byte
    mpuToProcess.mpuIntStatus = mpuToProcess.mpu.getIntStatus();

    // get current FIFO count
    mpuToProcess.fifoCount = mpuToProcess.mpu.getFIFOCount();

    // check for overflow (this should never happen unless our code is too inefficient)
    if ((mpuToProcess.mpuIntStatus & 0x10) || mpuToProcess.fifoCount == 1024) {
        // reset so we can continue cleanly
        mpuToProcess.mpu.resetFIFO();
    } else if (mpuToProcess.mpuIntStatus & 0x02) {
        // wait for correct available data length, should be a VERY short wait
        while (mpuToProcess.fifoCount < mpuToProcess.packetSize) mpuToProcess.fifoCount = mpuToProcess.mpu.getFIFOCount();

        // read a packet from FIFO
        mpuToProcess.mpu.getFIFOBytes(mpuToProcess.fifoBuffer, mpuToProcess.packetSize);
        
        // track FIFO count here in case there is > 1 packet available
        // (this lets us immediately read more without waiting for an interrupt)
        mpuToProcess.fifoCount -= mpuToProcess.packetSize;
    
        // display quaternion values in InvenSense Teapot demo format:
        mpuToProcess.teapotPacket[2] = mpuToProcess.fifoBuffer[0];
        mpuToProcess.teapotPacket[3] = mpuToProcess.fifoBuffer[1];
        mpuToProcess.teapotPacket[4] = mpuToProcess.fifoBuffer[4];
        mpuToProcess.teapotPacket[5] = mpuToProcess.fifoBuffer[5];
        mpuToProcess.teapotPacket[6] = mpuToProcess.fifoBuffer[8];
        mpuToProcess.teapotPacket[7] = mpuToProcess.fifoBuffer[9];
        mpuToProcess.teapotPacket[8] = mpuToProcess.fifoBuffer[12];
        mpuToProcess.teapotPacket[9] = mpuToProcess.fifoBuffer[13];
        mpuToProcess.teapotPacket[10] = mpuID;
        Serial.write(mpuToProcess.teapotPacket, 14);
        mpuToProcess.teapotPacket[11]++; // packetCount, loops at 0xFF on purpose
    }
}

void loop() {
    for (byte i = 0; i < NUM_MPUS; i++) {
      tcaselect(mpuIndex[i]);
      processMPU(mpus[i], mpuIndex[i]);
    }
}

