## BLE Robot Car Control

...........![Application screen](doc/blerc_app2.jpg)

### Purpose

This work is an educational project on creating a mobile application for controlling a robot car (radio-controlled toy) using the BLE protocol (Bluetooth Low Energy). 

### Tools used

BLE-client:
- Android Studio v.2025.2.3,
- programming language Kotlin,
- user interface by Jetpack Compose toolkit,
- target platform - Android 10 (API Level 29),
- Java SDK version - JDK24.

BLE-server:
- Arduino IDE v.2.3.7., framework Arduino ESP32 Core,
- programming language С++,
- built-in library Arduino BLE.

Debug:
- HP Pavilion Desktop PC 570 (Intel Core i3-7100 CPU 3.9 GHz, RAM 8Gb, Win10),
- smartphone Honor 10 Lite HRY-LX1,
- microcontroller development board ESP32C3 (AirM2M Core ESP32C3) from Aliexpress.

### Job Description

According to the interaction scenario, the server (the robotic car's microcontroller) initializes the BLE service, enables advertising, and waits for commands from the client (smartphone). The control commands are:
- "F" (forward),
- "B" (backward),
- "R" (right),
- "L" (left),
- "S" (stop).

The client scans the network, finds a server using the known MAC address of the device, performs the "Connect" operation and provides the user with the ability to control the robot car using the appropriate buttons (commands).

### Implementation of a BLE client

User interaction is limited to a single screen. The application layout (Mainscreen.kt file) includes a header, a connection status bar with "Scan" and "Connect/Disconnect" buttons, a control panel, and a scrollable event log window. A button for clearing the event log has also been added to the upper-right corner of the screen.
Специфичекие для приложения классы вынесены в отдельный файл Data.kt:
- BLEDevice - BLE device attributes,
- ConnectionState - connection status,
- ControlCommand - control command,
- LogEntry, Logtype - form an event log line.

The functions that provide interaction with the BLE server, as well as writing to the event log, are located in the MainViewModel.kt file. Working with the android.bluetooth API used in the application is described in detail here: https://punchthrough.com/android-ble-guide/. 
The permissions required by an app to perform BLE tasks are declared in the AndroidManifest.xml file. The list of permissions is as follows:
- "android.permission.BLUETOOTH",
- "android.permission.BLUETOOTH_ADMIN",
- "android.permission.BLUETOOTH_SCAN",
- "android.permission.BLUETOOTH_CONNECT",
- "android.permission.BLUETOOTH_ADVERTISE",
- "android.permission.ACCESS_COARSE_LOCATION",
- "android.permission.ACCESS_FINE_LOCATION".

The last two permissions on the list must be explicitly granted to the app through the smartphone's user interface. The app will not use the location for anything other than enabling BLE scanning. The app uses the checkPermissions function to verify permissions.

Scan results, connection status determination and BLE service discovery are performed through the callback method.\
Writing a characteristic (control command) to the BLE server is done in a confirmation mode.\
The mobile application was debugged on a smartphone connected to a USB port.

### Implementing a BLE server

The server's functionality is contained in the Air_BLE_my.ino file, which the Arduino IDE compiles and uploads the resulting code to the microcontroller of the ESP32C3 development board, which is connected to the computer's USB port.
The program was written using a tutorial example from the built-in Arduino IDE library. Designing a BLE server involves:
1. Create a BLE Server
2. Create a BLE Service
3. Create a BLE Characteristic on the Service
4. Create a BLE Descriptor on the characteristic
5. Start the service.
6. Start advertising.

The robotic car platform has two DC motors controlled by an L298N driver. The setup process begins with configuring the microcontroller board pins used to control this driver: ENA, ENB, IN1, IN2, IN3, and IN4.\
The board's onboard LEDs, LED1 and LED2, indicate the execution of control commands received from the client.\
After initializing and starting the BLE service, the program enters a mode that listens for control commands. Command reception and execution are implemented by a callback function, in response to the client recording the BLE characteristic.\
Motor operation modes are defined in separate procedures.\
Connection status and control commands during debugging are displayed in the Serial Monitor window of the Arduino IDE.\
If the connection is lost (the "Disconnect" operation), the program resumes advertising the host on the network, ensuring the client rescans.