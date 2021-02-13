# Liberty-Way AMLS Landing Controller


![Logo](https://github.com/XxOinvizioNxX/Liberty-Way/blob/main/git_images/logo_book.png "Logo")

Project created as part of project activities at the Moscow Polytechnic University in the group 181-311, as well as a part for the CopterHack 2021 from Copter Express.

Liberty-Way AMLS Landing Controller © 2021 Pavel Neshumov (Frey Hertz)

All the code excluding the dependencies block, was written by Pavel Neshumov (Frey Hertz)

------------

### Dependencies
- **Flak** (Web framework): https://github.com/pcdv/flak
- **MiniPID** (PID Controller): https://github.com/tekdemo/MiniPID-Java
- **Log4j** (Logback): https://logging.apache.org/log4j/1.2/
- **OpenCV** (Computer vision): https://github.com/opencv/opencv
- **OpenCV-contib** (Extra OpenCV modules): https://github.com/opencv/opencv_contrib
- **Commons-CLI** (Arguments parser): https://commons.apache.org/proper/commons-cli/
- **jSerialComm** (Serial communication): https://github.com/Fazecast/jSerialComm
- **GSon** (JSON implementation): https://github.com/google/gson

------------

### Description
This application is part of the AMLS project (Autonomous Multi-rotor Landing System).
The task of the project is to automatically land the drone on a platform in motion.

![Screenshot](https://github.com/XxOinvizioNxX/Liberty-Way/blob/main/git_images/Screenshot_1.png "Screenshot")

This application processes the frame received from the camera located on the platform (the camera is looking up). On the bottom of the drone, there is an ARUco tag. The application detects the marker, estimates its position. Then passes through the PID controller and sends the correction values to the drone.

------------

### Building and running
Liberty Way is a cross-platform application and has been tested on Linux and Windows. You can try running the application on any other operating system. But you first need to build the OpenCV-contrib library (The binaries include libraries for Windows and Linux).
Binaries can be found in releases as well as in the main branch (Liberty-Way.jar file)

All control is performed only from the browser.
When starting the application, you can specify the arguments:
```
 -c,--color                write colored logs.
 -i,--ip <arg>             server ip
 -sp,--server_port <arg>   web server port (0 - 65535)
 -vp,--video_port <arg>    video stream port (0 - 65535)
```
Also, the server address and ports can be specified in the settings file (settings.json)
