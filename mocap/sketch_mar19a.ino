#define PJON_MAX_PACKETS 10
#define SWBB_MAX_ATTEMPTS 50

#include <PJON.h>
#include <PJONMaster.h>

PJONMaster<SoftwareBitBang> bus;

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
  if((char)payload[0] == '$') {
    int clientId = packet_info.sender_id;

    Serial.print(clientId);
    Serial.print(F(": "));
    Serial.write(payload, length);
    Serial.print(F(" Length: "));
    Serial.print(length);
    Serial.print(F(" Updated: "));
    Serial.println(payload[length -1]);
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
  Serial.begin(9600);

  bus.strategy.set_pin(12);
  bus.set_receiver(receiver_function);
  bus.set_error(error_handler);
  bus.begin();
}

void loop() { 
  if (Serial.available()) {
    char command = Serial.read();
    switch (command) {
      case 'I': {
        int deviceId = Serial.parseInt();
        
        char packet[1] = {'I'};        
        bus.send(deviceId, packet, sizeof(packet));
        
        break;
      }
    }
    Serial.flush();
  }
  
  
  bus.receive(50000);
  bus.update();
}
