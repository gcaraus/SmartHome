# SmartHome

Open source Smart Home Automation using IoT solution
This project serves to as link and starting ground for future projects where users can use microcontrller boards (Arduino in this case)
to collect data from sensors and take actions with actuators by means of an Android application.

The communication with the microontrolers is done via Bluetooth,and values transmited are oploaded for storage and future operations 
in a MySQL database.

At the moment the entire situation is like this :
1. Arduinos monitor ambient temperature via a sensor. 
2. If value of temperature reaches a certain top limit the microcontroller will lower the window blinds
3. Subsequently the sensor value is sent to the Android application for user to visualize 
4. If user will want to lower blinds himself this can be done via the app as well

The amount of functionalities can be extended with ease. 
