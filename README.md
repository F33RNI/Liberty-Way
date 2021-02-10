# Liberty-Way AMLS Landing Controller

Project created as part of project activities at the Moscow Polytechnic University in the group 181-311, as well as a part for the CopterHack 2021 from Copter Express.

Liberty-Way AMLS Landing Controller © 2021 Pavel Neshumov (Frey Hertz)

All the code excluding the dependencies block, was written by Pavel Neshumov (Frey Hertz)

------------

### Description
This application is part of the AMLS project (Autonomous Multi-rotor Landing System).
The task of the project is to automatically land the drone on a platform in motion.

This application processes the frame received from the camera located on the platform (the camera is looking up). On the bottom of the drone, there is an ARUco tag. The application detects the marker, estimates its position. Then passes through the PID controller and sends the correction values to the drone.

------------

### Dependencies
- Flak (Web framework): https://github.com/pcdv/flak
- Log4j (Logback): https://logging.apache.org/log4j/2.x/
- OpenCV (Computer vision): https://github.com/opencv/opencv
- OpenCV-contib (Extra OpenCV modules): https://github.com/opencv/opencv_contrib
- Commons-CLI (Arguments parser): https://commons.apache.org/proper/commons-cli/
- jSerialComm (Serial communication): https://github.com/Fazecast/jSerialComm
- GSon (JSON implementation): https://github.com/google/gson


