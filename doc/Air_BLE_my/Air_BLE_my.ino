/*-----------------------------------------------------------------------------------------------------
 AirBLEmy.ino
 Robot-car, модуль драйвера L298N. Управление с помощью BLE. 
-----------------------------------------------------------------------------------------------------
   Create a BLE server. The design of creating the BLE server is:
   1. Create a BLE Server
   2. Create a BLE Service
   3. Create a BLE Characteristic on the Service
   4. Create a BLE Descriptor on the characteristic
   5. Start the service.
   6. Start advertising.
*/

#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>

// подключение модуля драйвера L298N (дополнительно: GND, Vin, +5V, M1, M2)
#define ENA 6  // включение правого (по ходу движения) мотора, ШИМ
#define IN1 1  // вращение правого мотора
#define IN2 12 // вращение правого мотора
#define IN3 18 // вращение левого мотора
#define IN4 19 // вращение левого мотора
#define ENB 8  // включение левого мотора, ШИМ

#define MIN_SPEED 100         // минимальная скорость моторов; если меньше, моторы не смогут вращаться
#define MAX_SPEED 255         // максимальная скорость
#define REGULAR_SPEED 140     // регулярная скорость
#define LED1 12
#define LED2 13

// ESP32 BLE Server definition
#define SERVICE_UUID           "C6FBDD3C-7123-4C9E-86AB-005F1A7EDA01"  // UART service UUID
#define CHARACTERISTIC_UUID_RX "B88E098B-E464-4B54-B827-79EB2B150A9F"
#define CHARACTERISTIC_UUID_TX "D769FACF-A4DA-47BA-9253-65359EE480FB"

BLEServer *pServer = NULL;
BLECharacteristic *pTxCharacteristic;
bool deviceConnected = false,
     reverse = false; 
uint8_t txValue = 0,
        speed = 0,
        command = 'S',
        prev_command = 'S';
String str = "";

const int frequency = 30000; // частота ШИМ
const int pwmChan1 = 0;     // канал ШИМ1
const int pwmChan2 = 1;     // канал ШИМ2
const int resolution = 8;   // разрешение ШИМ (в битах)   

/////////////////////////////////////////
// варианты работы моторов:
// левый мотор вперед
void LMgo(uint8_t sp) {
  digitalWrite(IN1, HIGH);
  digitalWrite(IN2, LOW);
  ledcWrite(ENA, sp); 
  digitalWrite(LED2, HIGH);
}

// правый мотор вперед
void RMgo(uint8_t sp) {
  digitalWrite(IN3, HIGH);
  digitalWrite(IN4, LOW);
  ledcWrite(ENB, sp);
  digitalWrite(LED1, HIGH);
}

// левый мотор назад
void LMback(uint8_t sp) {
  digitalWrite(IN1, LOW);
  digitalWrite(IN2, HIGH);
  ledcWrite(ENA, sp);
  digitalWrite(LED2, LOW);
}

// правый мотор назад
void RMback(uint8_t sp) {
  digitalWrite(IN3, LOW);
  digitalWrite(IN4, HIGH);
  ledcWrite(ENB, sp);
  digitalWrite(LED1, LOW);
}

// оба мотора стоп
void Stop() {  
  ledcWrite(ENA, 0);
  digitalWrite(IN1, LOW);
  digitalWrite(IN2, LOW);
  ledcWrite(ENB, 0);
  digitalWrite(IN3, LOW);
  digitalWrite(IN4, LOW);
  digitalWrite(LED1, LOW);
  digitalWrite(LED2, LOW);
}
void Blink() {
  digitalWrite(LED2, HIGH);
  delay(500);
  digitalWrite(LED2, LOW);
  digitalWrite(LED1, HIGH);
  delay(500);
  digitalWrite(LED1, LOW);
}

class MyServerCallbacks : public BLEServerCallbacks {
  void onConnect(BLEServer *pServer) {
    deviceConnected = true;
    Serial.println("\nSome client connected");
  };

  void onDisconnect(BLEServer *pServer) {
    deviceConnected = false;
    Serial.println("The client disconnected\n");
    pServer->getAdvertising()->start();
    Serial.println("Waiting a client connection ...");
    Blink();
  };
};

class MyCallbacks : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic *pCharacteristic) {
    String rxValue = pCharacteristic->getValue().c_str();  // Use Arduino String
    if (rxValue.length() > 0) {
      Serial.print("Received ");
      Serial.print("\'" + rxValue + "\'(");
      for (int i = 0; i < rxValue.length(); i++) {
        command = (char) toupper(rxValue[i]);
        Serial.print(command);
        switch(command) {
          case 70:  LMgo(speed);       // 'F' вперед
                    RMgo(speed);   
                    Serial.println(") - moving forward");
                    break;  
          case 66:  LMback(speed);     // 'B' назад
                    RMback(speed);
                    Serial.println(") - moving backward");
                    break;
          case 76:  RMgo(MIN_SPEED);   // 'L' танковый разворот налево
                    LMback(MIN_SPEED);
                    Serial.println(") - moving left");
                    break; 
          case 82:  LMgo(MIN_SPEED);   // 'R' танковый разворот направо
                    RMback(MIN_SPEED);
                    Serial.println(") - moving right");
                    break; 
          case 71:  RMgo(speed);   // 'G' вперед и налево
                    LMgo(speed/2);
                    Serial.println(" - moving forward-left");
                    break;
          case 73:  LMgo(speed);   // 'I' вперед и направо
                    RMgo(speed/2);
                    Serial.println(") - moving forward-right");
                    break;
          case 72:  str = "Moving back-left";
                    RMback(speed); // 'H' назад и налево
                    LMback(speed/2);
                    break;
          case 74:  LMback(speed); // 'J' назад и направо
                    RMback(speed/2);
                    Serial.println(") - moving back-left");
                    break;
          case 83:  Stop();        // 'S' стоп
                    speed = 100; 
                    Serial.println(") - stop engine!");
                    Blink();  
        }
        delay(100);
        rxValue = "";
        prev_command = command;
        command = 0;  
      }
    }
  }
};

void setup() {
  Serial.begin(115200);
  pinMode(LED1, OUTPUT);
  pinMode(LED2, OUTPUT);
  pinMode(ENA, OUTPUT);
  pinMode(ENB, OUTPUT);
  pinMode(IN1, OUTPUT);
  pinMode(IN2, OUTPUT);
  pinMode(IN3, OUTPUT);
  pinMode(IN4, OUTPUT);
  ledcAttachChannel(ENA, frequency, resolution, pwmChan1); // задаём настройки ШИМ1
  ledcAttachChannel(ENB, frequency, resolution, pwmChan2); // задаём настройки ШИМ2
  Stop();   // motor Stop
  Serial.println("BLE car control");

  // Create the BLE Device
  BLEDevice::init("BLE_CAR"); // BLE device name

  // Create the BLE Server
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  // Create the BLE Service
  BLEService *pService = pServer->createService(SERVICE_UUID);

  // Create a BLE Characteristic
  pTxCharacteristic = pService->createCharacteristic(
                        CHARACTERISTIC_UUID_RX,
                        BLECharacteristic::PROPERTY_NOTIFY
                      );

  BLECharacteristic *pRxCharacteristic = pService->createCharacteristic(
                                            CHARACTERISTIC_UUID_TX,
                                            BLECharacteristic::PROPERTY_WRITE
                                          );
  pRxCharacteristic->setCallbacks(new MyCallbacks());

  // Start the service
  pService->start();

  // Start advertising
  pServer->getAdvertising()->start();
  Serial.println("Waiting a client connection ...");
  Blink();
}

void loop() {
  delay(1000);
}