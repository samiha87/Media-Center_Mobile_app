# Media-Center controller
This app is for raspberry pi based media center. App uses BLE to connect raspberry. Media-Center on raspberry is running OSMC, python software to control pheripherals and Qt based program to keep track of AV-Equipments.

Mobile app connects to BLE. Python layer is keeping track of BLE communication, once message arrives it will transfer the message to logic(Qt program). Logic reads the message and reacts accordingly. If volume is controlled from Raspberry HDMI or headphones, logic will send correct command to python layer and volume is adjusted as requested.
