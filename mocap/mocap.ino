#include "Wire.h"
#include "I2Cdev.h"
#include "MPU6050_6Axis_MotionApps20.h"

#define TCAADDR 0x70
#define NUM_MPUS 2
#define CALIBRATION_CODE 1

#ifdef CALIBRATION_CODE
  #define HIST_SIZE 50
#endif

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

#ifdef CALIBRATION_CODE
  int16_t ax, ay, az;
  int16_t gx, gy, gz;
  
  float aax[HIST_SIZE];
  float aay[HIST_SIZE];
  float aaz[HIST_SIZE];
  
  float agx[HIST_SIZE];
  float agy[HIST_SIZE];
  float agz[HIST_SIZE];
  
  int16_t off_ax, off_ay, off_az;
  int16_t off_gx, off_gy, off_gz;
  
  int since = millis();
  int timestep = 0;
  byte calibrated = 0;
#endif

/** 
 *  Selects the given device in the multiplexer.
 */
void tcaselect(uint8_t i) {
  if (i > 7) return;
 
  Wire.beginTransmission(TCAADDR);
  Wire.write(1 << i);
  Wire.endTransmission();  
}

#ifdef CALIBRATION_CODE
float mean(float *data, int count) {
  int i;
  float total;
  float result;

  total = 0;
  for(i=0; i<count; i++)
  {
    total = total + data[i];
  }
  result = total / (float)count;
  return result;
}

float stddev(float *data, int count) {
  float square;
  float sum;
  float mu;
  float theta;
  int i;

  mu = mean(data,count);

  sum = 0;
  for(i=0; i<count; i++) {
    theta = (float)mu - (float)data[i];
    square = theta * theta;
    sum += square;
  }
  return sqrt(sum/(float)count);
}

void calibration(MPU6050& accelgyro){
  accelgyro.getMotion6(&ax, &ay, &az, &gx, &gy, &gz);
    
    off_ax = accelgyro.getXAccelOffset();
    off_ay = accelgyro.getYAccelOffset();
    off_az = accelgyro.getZAccelOffset();
    off_gx = accelgyro.getXGyroOffset();
    off_gy = accelgyro.getYGyroOffset();
    off_gz = accelgyro.getZGyroOffset();
    
    aax[timestep] = ax;
    aay[timestep] = ay;
    aaz[timestep] = az;
    agx[timestep] = gx;
    agy[timestep] = gy;
    agz[timestep] = gz;
    
    if (millis() - since < 180000) {
        printData();
        delay(1000);
        return;
    }

    if (ax > 0) off_ax--; else if (ax < 0) off_ax++;
    if (ay > 0) off_ay--; else if (ay < 0) off_ay++;
    if (az > 16384) off_az--; else if (az < 16384) off_az++;
    
    if (gx > 0) off_gx--; else if (gx < 0) off_gx++;
    if (gy > 0) off_gy--; else if (gy < 0) off_gy++;
    if (gz > 0) off_gz--; else if (gz < 0) off_gz++;
    
    accelgyro.setXAccelOffset(off_ax);
    accelgyro.setYAccelOffset(off_ay);
    accelgyro.setZAccelOffset(off_az);
    accelgyro.setXGyroOffset(off_gx);
    accelgyro.setYGyroOffset(off_gy);
    accelgyro.setZGyroOffset(off_gz);

    printData();
    timestep++;
    if (timestep >= HIST_SIZE) {
        timestep = 0;
    }
    delay(200);
}
#endif

void setup() {
    // join I2C bus (I2Cdev library doesn't do this automatically)
    Wire.begin();
    Wire.setClock(200000L);

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

        mpus[i].mpu.setXGyroOffset(0);
        mpus[i].mpu.setYGyroOffset(0);
        mpus[i].mpu.setZGyroOffset(0);
        mpus[i].mpu.setZAccelOffset(0);
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

void processMPU(MPU& mpuToProcess, byte mpuID) {
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
        //Serial.write(mpuToProcess.teapotPacket, 14);
        //mpuToProcess.teapotPacket[11]++; // packetCount, loops at 0xFF on purpose
    }
}

#ifdef CALIBRATION_CODE
void printData()
{
    Serial.print("a/g:\t");
    Serial.print(ax); Serial.print("\t");
    Serial.print(ay); Serial.print("\t");
    Serial.print(az); Serial.print("\t");
    Serial.print(gx); Serial.print("\t");
    Serial.print(gy); Serial.print("\t");
    Serial.print(gz); Serial.print("\t");
    Serial.print(off_ax); Serial.print("\t");
    Serial.print(off_ay); Serial.print("\t");
    Serial.print(off_az); Serial.print("\t");
    Serial.print(off_gx); Serial.print("\t");
    Serial.print(off_gy); Serial.print("\t");
    Serial.print(off_gz); Serial.print("\t");
    Serial.print(mean(aax, HIST_SIZE)); Serial.print("\t");
    Serial.print(mean(aay, HIST_SIZE)); Serial.print("\t");
    Serial.print(mean(aaz, HIST_SIZE)); Serial.print("\t");
    Serial.print(mean(agx, HIST_SIZE)); Serial.print("\t");
    Serial.print(mean(agy, HIST_SIZE)); Serial.print("\t");
    Serial.print(mean(agz, HIST_SIZE)); Serial.print("\t");
    Serial.print(stddev(aax, HIST_SIZE)); Serial.print("\t");
    Serial.print(stddev(aay, HIST_SIZE)); Serial.print("\t");
    Serial.print(stddev(aaz, HIST_SIZE)); Serial.print("\t");
    Serial.print(stddev(agx, HIST_SIZE)); Serial.print("\t");
    Serial.print(stddev(agy, HIST_SIZE)); Serial.print("\t");
    Serial.print(stddev(agz, HIST_SIZE));
    Serial.println("");
}
#endif

void loop() {
  #ifdef CALIBRATION_CODE
  if (!calibrated) {
    for (byte i = 0; i < NUM_MPUS; i++) {
        tcaselect(mpuIndex[i]);
    
        Serial.println(F("Calibrating..."));
        for (byte l = 0; l < 10; l++) {
          calibration(mpus[i].mpu);
        }
        mpus[i].mpu.setXAccelOffset(off_ax);
        mpus[i].mpu.setYAccelOffset(off_ay);
        mpus[i].mpu.setZAccelOffset(off_az);
        mpus[i].mpu.setXGyroOffset(off_gx);
        mpus[i].mpu.setYGyroOffset(off_gy);
        mpus[i].mpu.setZGyroOffset(off_gz);
        delay(1000);
    }
    delay(1000);
    calibrated = 1;
  }
  #endif
  
    for (byte i = 0; i < NUM_MPUS; i++) {
      tcaselect(mpuIndex[i]);
      processMPU(mpus[i], mpuIndex[i]);
    }

    for (uint8_t i = 0; i < NUM_MPUS; i++) {
      Serial.write(mpus[i].teapotPacket, 14);
    }
}

