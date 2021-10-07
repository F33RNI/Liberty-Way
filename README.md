# Liberty-Way AMLS Landing Controller

![Logo](/git_images/logo_book.png "Logo")

[![Twitter](https://img.shields.io/twitter/url?color=gray&label=twitter&logo=twitter&style=flat-square&url=https%3A%2F%2Ftwitter.com%2Fliberty_drones)](https://twitter.com/liberty_drones)
[![GitHub license](https://img.shields.io/github/license/XxOinvizioNxX/Liberty-Way?color=red&style=flat-square)](https://github.com/XxOinvizioNxX/Liberty-Way/blob/main/LICENSE)
[![GitHub issues](https://img.shields.io/github/issues/XxOinvizioNxX/Liberty-Way?color=orange&style=flat-square)](https://github.com/XxOinvizioNxX/Liberty-Way/issues)
![GitHub All Releases](https://img.shields.io/github/downloads/XxOinvizioNxX/Liberty-Way/total?color=yellow&style=flat-square "GitHub All Releases")
[![Travis (.com)](https://img.shields.io/travis/com/XxOinvizioNxX/Liberty-Way?color=green&style=flat-square)](https://www.travis-ci.com/github/XxOinvizioNxX)
[![GitHub stars](https://img.shields.io/github/stars/XxOinvizioNxX/Liberty-Way?color=blue&style=flat-square)](https://github.com/XxOinvizioNxX/Liberty-Way/stargazers)
[![Liberty-Way_beta_3.0.0](https://img.shields.io/badge/latest_version-beta_3.0.0-informational?logo=Github&style=flat-square&color=purple "Liberty-Way_beta_3.0.0")](https://github.com/XxOinvizioNxX/Liberty-Way/releases/tag/beta_3.0.0 "Liberty-Way_beta_3.0.0")

This project is a part of project activity in Moscow Polytech University by students of group 181-311

Liberty-Way AMLS Landing Controller © 2021 Pavel Neshumov (Fern H.)

All the code excluding the dependencies block, was written by Pavel Neshumov (Fern H.)

----------

## Table of contents

- [More projects](#more-projects)
  - [Liberty-X](#liberty-x)
  - [Eitude](#eitude)
  - [Sonarus](#sonarus)
  - [GPS Mixer](#gps-mixer)
  - [GPS to Serial](#gps-to-serial)
- [Feedback](#feedback)
- [For project development](#for-project-development)
- [Dependencies](#dependencies)
- [Description](#description)
- [Licenses](#licenses)
- [Logotype](#logotype)
- [Building and running](#building-and-running)
- [Configuration](#configuration)
  - [Settings](#settings)
  - [PID](#pid)
- [Data packet structure](#data-packet-structure)
  - [IDLE (Link command 0)](#idle-link-command-0)
  - [Direct control (Link command 1)](#direct-control-link-command-1)
  - [Pressure Waypoint (Link command 2)](#pressure-waypoint-link-command-2)
  - [GPS waypoint (Link command 3)](#gps-waypoint-link-command-3)
  - [Motors stop (Link command 4)](#motors-stop-link-command-4)
  - [Start Liberty-Way sequence (Link command 5)](#start-liberty-way-sequence-link-command-5)
  - [Abort (Link command 6)](#abort-link-command-6)
- [TODO](#todo)
----------

## More projects
### Liberty-X
Drone flight controller
- https://github.com/XxOinvizioNxX/Liberty-X

### Eitude
AMLS Platform controller
- https://github.com/XxOinvizioNxX/Liberty-Way/tree/main/Eitude

### Sonarus
I2C ultrasonic rangefinder
- https://github.com/XxOinvizioNxX/Liberty-Way/tree/main/Sonarus

### GPS Mixer
Way to improve GPS navigation using multiple receivers
- https://github.com/XxOinvizioNxX/Liberty-Way/tree/main/GPS-mixer

### GPS to Serial
Android app to send phone GPS coordinates via USB serial port
- https://github.com/XxOinvizioNxX/GPS-to-Serial

----------

## Feedback

- Fern H. (Pavel Neshumov) (Author and CEO of the project) E-Mail: xxoinvizionxx@gmail.com, Twitter: @fern_hertz
- Andrey Kabalin (Project developer. GPS stabilization, hardware development) E-Mail: astik452@gmail.com
- Vladislav Yasnetsky (Project developer. Video editing, PR, hardware development). E-Mail: vlad.yasn@gmail.com

## For project development

- BTC: `bc1qd2j53p9nplxcx4uyrv322t3mg0t93pz6m5lnft`
- ZEC: `t1Jb5tH61zcSTy2QyfsxftUEWHikdSYpPoz`
- ETH: `0x284E6121362ea1C69528eDEdc309fC8b90fA5578`

----------

## Dependencies

- **Flak** (Web framework): https://github.com/pcdv/flak
- **MiniPID** (PID Controller): https://github.com/tekdemo/MiniPID-Java
- **Log4j** (Logback): https://logging.apache.org/log4j/1.2/
- **OpenCV** (Computer vision): https://github.com/opencv/opencv
- **OpenCV-contib** (Extra OpenCV modules): https://github.com/opencv/opencv_contrib
- **Commons-CLI** (Arguments parser): https://commons.apache.org/proper/commons-cli/
- **jSerialComm** (Serial communication): https://github.com/Fazecast/jSerialComm
- **GSon** (JSON implementation): https://github.com/google/gson
- **Augmented-UI** (WEB framework for Cyberpunk-style UI): https://github.com/propjockey/augmented-ui
- **Bing Maps** (Maps from Microsoft): https://www.bing.com/maps/

----------

## Description

This application is a part of the AMLS project (Autonomous Multi-rotor Landing System).
The task of the project is to automatically land a drone on a platform in motion.

![Screenshot](https://github.com/XxOinvizioNxX/Liberty-Way/blob/main/git_images/main_preview.png "Screenshot")

This application processes the frame received from the camera located on the platform which is looking up. On the bottom of the drone, there is an ARUco tag. The application detects the marker, estimates its position, then passes through the PID controller and sends the correction values to the drone.

----------

## Licenses

Liberty-Way is licensed under AGPLv3, you can find it’s contents in the main branch.

The dependencies are certificated by following licenses:

- Apache-2.0 for Flak, Log4j, Opencv, OpenCV-contib, Commons-CLI and GSon
- BSD-2-Clause License for Augmented-UI
- GPL-3.0 for MiniPID
- GNU-3.0 for jSerialComm
- Mapbox: https://www.mapbox.com/pricing/

----------

## Logotype

AMLS and Liberty-X logo was designed by Pavel Neshumov (Fern H.)

----------

## Building and running

Liberty-Way is a cross-platform application and has been tested on:
- Microsoft Windows 10 Pro 10.0.19041 N/A Build 19041, Intel(R) Core(TM) i7-9750H (x64)
- 5.6.0-kali2-amd64 SMP Debian 5.6.14-1kali1 (2020-05-25) x86_64 GNU/Linux, Intel(R) Core(TM) i7-9750H (x64)

You can run Liberty-Way on other operating systems. But it is strongly recommended to build the native OpenCV library from source for your operating system. Native libraries compiled on the listed operating systems are **contained in the release archives**, as well as **in the main branch**. If you build your native library, **put the library file next to the Liberty-Way.jar file**. Don't rename the native library file.

Commands to build native OpenCV library on Linux:

Install dependecies:

```
sudo apt-get install build-essential
sudo apt-get install cmake git libgtk2.0-dev pkg-config libavcodec-dev libavformat-dev libswscale-dev
sudo apt-get install python-dev python-numpy libtbb2 libtbb-dev libjpeg-dev libpng-dev libtiff-dev libjasper-dev libdc1394-22-dev
```

Install Ant:

```
sudo apt-get install ant
```

Install JDK:

```
sudo mkdir /usr/lib/jvm
cd /usr/lib/jvm
wget https://download.oracle.com/java/17/latest/jdk-17_linux-x64_bin.tar.gz
sudo tar -xvzf jdk-17_linux-x64_bin.tar.gz


export J2SDKDIR="/usr/lib/jvm/jdk-17"
export J2REDIR="/usr/lib/jvm/jdk-17/jre"
export JAVA_HOME="/usr/lib/jvm/jdk-17"
export DERBY_HOME="/usr/lib/jvm/jdk-17/db"
```

Configure your operating folder:

```
cd ~

mkdir opencv
cd opencv
```

Build the library itself:

```
wget https://github.com/opencv/opencv/archive/4.5.3.tar.gz
mv 4.5.3.tar.gz opencv-4.5.3.tar.gz
wget https://github.com/opencv/opencv_contrib/archive/4.5.3.tar.gz
mv 4.5.3.tar.gz opencv_contrib-4.5.3.tar.gz
tar -xf opencv-4.5.3.tar.gz
tar -xf opencv_contrib-4.5.3.tar.gz
cd opencv-4.5.3
mkdir build
cd build
mkdir install

cmake .. \
  -D CMAKE_SYSTEM_PROCESSOR=arm64 \
  -D CMAKE_OSX_ARCHITECTURES=arm64 \
  -D WITH_IPP=OFF \
  -D BUILD_opencv_java=ON \
  -D CMAKE_BUILD_TYPE=RELEASE \
  -D CMAKE_INSTALL_PREFIX=~/opencv/opencv-4.5.3/build/install \
  -D OPENCV_EXTRA_MODULES_PATH=~/opencv/opencv_contrib-4.5.3/modules \
  -D BUILD_opencv_python2=OFF \
  -D BUILD_opencv_python3=OFF \
  -D INSTALL_PYTHON_EXAMPLES=OFF \
  -D INSTALL_C_EXAMPLES=OFF \
  -D OPENCV_ENABLE_NONFREE=ON \
  -D BUILD_EXAMPLES=OFF \
  -D OPENCV_JAVA_TARGET_VERSION=1.8 \
  -D BUILD_SHARED_LIBS=OFF \
  -D BUILD_FAT_JAVA_LIB=ON
  
make
```

You can build the Liberty-Way application yourself or **run a ready-made .jar from releases (recommended)**. Also, **nightly builds** (.jar) can be downloaded from main branch.

To run the application, you must use command `java -Djava.library.path=. -jar "./Liberty-Way.jar" -c ""` or lauch file `START_WINDOWS.bat` or `START_LINUX.sh` or `START_MACOS.sh` (depends on your operating system)

Examples of commands which build Liberty-Way on Linux:

```
sudo apt-get install git
sudo apt-get install ant
git clone https://github.com/XxOinvizioNxX/Liberty-Way
cd Liberty-Way
ant main
```

Liberty-Way is a cross-platform application and has been **tested on Linux and Windows**. You can try running the application on any other operating system. But you first need to build the OpenCV-contrib library (**The releases and main branch include libraries for Windows and Linux**).

All of the controls is performed only from the browser.
When starting the application, you can specify the arguments:

```
 -c,--color                write colored logs.
 -i,--ip <arg>             server ip
 -sp,--server_port <arg>   web server port (0 - 65535)
 -vp,--video_port <arg>    video stream port (0 - 65535)
```

Also, the server address and ports can be specified in the configuration (settings.json)

When launched, the console displays the IP address of the user interface. You need to go to it in your browser

----------

## Configuration

### Settings

These are the parameters presented in settings.json file that are used by the program and can be changed depending of its application:

`"marker_size": 5.0` - length of one side of the tracking marker (cm)

`"max_exposure": -8` - default and maximum exposure value of the camera

`"motors_turn_off_height": 25.0` - altitude of sending a packet to turn off the motors

`"landing_allowed": true` - whether to lower the drone during optical stabilization. If set to false, the drone will never land

`"only_optical_stabilization": false` - if true, Liberty-Way will not send start commands and waypoints

`"max_marker_height": 200` - marker will not be calculated if found above this height

`"pid_file": "pid.json"` - file of PID configuration

`"camera_matrix_file": "camera_matrix.json"` - camera calibration (matrix)

`"camera_distortions_file": "camera_distortions.json"` - camera calibration (distortions)

`"watermark_file": "watermark.png"` - image of a watermark (top-right corner)

`"web_resources_folder": "web/static"` - folder of web resources (static folder)

`"web_templates_folder": "web/templates"` - folder of templates (html) for a web-page

`"blackbox_folder": "blackbox/"` - folder which stores blackbox .csv files

`"frame_width": 1280` - resolution that is set to web-camera (width)

`"frame_height": 720` - resolution that is set to web-camera (height)

`"disable_auto_exposure": true` - disabling/enabling auto exposure feature

`"disable_auto_wb": true` - disabling/enabling auto white balance feature

`"disable_auto_focus": true` - disabling/enabling auto focus feature

`"default_server_host": "localhost"` - server host which can be overridden by cmd argument

`"default_server_port": 80` - server port which can be overridden by cmd argument

`"default_video_port": 8080` - video port which can be overridden by cmd argument

`"video_stream_enabled_by_default": true` - should the video be enabled on the page from the start

`"blackbox_enabled": true` - should the blackbox feature be enabled by default

`"serial_reconnect_time": 500` - how many milliseconds to try to open the serial port if it is lost

`"udp_timeout": 2000` - UDP response timeout (milliseconds)

`"telemetry_lost_time": 3000` - how many milliseconds it takes to consider telemetry (drone) lost

`"platform_lost_time": 1000` - how many milliseconds it takes to consider platform lost

`"platform_light_enable_threshold": 1000` - lower threshold of the surrounding light to enable additional lighting

`"platform_light_disable_threshold": 3000` - upper threshold of the surrounding light to disable additional lighting

`"platform_loop_timer": 100` - time of one cycle of receiving data from the platform

`"fps_measure_period": 2000` - how many milliseconds to measure FPS

`"adaptive_thresh_constant": 15` - detector of parameters (ARUCO)

`"aruco_dictionary": 0` - index of used ARUco dictionary (default = 0 which is 50 4x4 marks) 

`"allowed_ids": [9]` - array of allowed tracking markers ARUCO ids

`"input_filter": 0.30` - Kalman filter coefficient

`"setpoint_alignment_factor": 0.75` - floating setpoint to the desired position coefficient

`"allowed_lost_frames": 10` - frames where the marker is not in the input frame

`"landing_decrement": 0.20` - constant latitude decrement (cm)

`"allowed_landing_range_xy": 5.0` - range of the landing allowance (cm)

`"allowed_landing_range_yaw": 5.0` - range of the landing allowance (degrees)

`"min_satellites_num_start": 0` - the minimum number of sattelites is set for the start

`"min_satellites_num": 0` - the minimum number of sattelites allowed

`"setpoint_x": 0.0` - setpoint of PID controller (absolute estimated x)

`"setpoint_y": 0.0` - setpoint of PID controller (absolute estimated y)

`"setpoint_yaw": 0.0` - setpoint of PID controller (absolute estimated yaw)

`"drone_data_suffix_1": 238` - first of the unique pair symbols that show the end of the packet

`"drone_data_suffix_2": 239` - second of the unique pair symbols that show the end of the packet

`"platform_data_suffix_1": 238` - first of the unique pair symbols that show the end of the packet

`"platform_data_suffix_2": 239` - second of the unique pair symbols that show the end of the packet

`"push_osd_after_frames": 2` - after how many frames the image is being sent to the web-page

`"planet_radius": 6378.137` - the radius of the planet the project is running on

`"pressure_term_above_platform": 30` - amount of pressure (Pa) above the platform which is equivalent to 3 meters above the ground

`"send_idle_cycles_num": 0` - amount of cycles in which telemetry data is sent between GPS data (0 - only GPS data)

`"is_telemetry_necessary": true` - is it necessary to use telemetry

`"max_platform_speed": 10` - the allowed maximum speed for the platform

`"send_idle_in_wait_mode": true` - is it necessary to send telemetry data while the drone isn't active

`"is_gps_prediction_allowed": false` - is it necessary to predicr future positions

`"stop_prediction_on_distance": 10` - amount of meters on which the prediction of future positions stops operating

`"log_fps": false` - is it necessary to log fps while operating

`"log_api_requests": false` - is it necessary to log processed POST requests to API

### PID

These are the PID regulation parameters for each processed axle (x, y, z and yaw) which can be found in pid.json file:

```
"P",              proportional term coefficient 
"I",              integral term coefficient 
"D",              derivative term coefficient
"F",              feed-forward term (which is a rough prediction of the output value) coefficient
"ramp",           maximum rate that the output can increase per cycle
"limit",          acceptable maximum of the output value
"reversed",       should the output be in a reversed state with opposite value
```

----------

## Data packet structure

Structure of Liberty-Link packets that Liberty-Way sends to the drone

Each packet consists of 12 bytes. The first 8 bytes are the payload. Next comes 1 byte Link Command, which tells the drone what to do with this packet. Then 1 byte XOR of the check-sum of the previous 9 bytes. At the end there are 2 bytes indicating the end of the packet (indicated in three. By default it is 0xEE 0xEF).

For each packet sent (even IDLE), the drone returns 1 or several bytes (the number is specified in the Liberty-X settings) of telemetry.

All data that is sent to the drone or comes from the drone (telemetry) is arranged in **big-endian order**.

----------

### IDLE (Link command 0)
In IDLE mode, the drone does not perform any new calculations and simply continues its flight in the previous mode.

The payload bytes must be equal to 0, the Link Command byte is equal to 0, then the check-sum (0) and 2 bytes of the end of the packet.

The table below shows the detailed structure of the packet

| Byte N | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Byte name | Payload byte 0 | Payload byte 1 | Payload byte 2 | Payload byte 3 | Payload byte 4 | Payload byte 5 | Payload byte 6 | Payload byte 7 | Link command byte | XOR check-sum | Packet suffix 1 | Packet suffix 2 |
| Description for IDLE | Should be 0 | Should be 0 | Should be 0 | Should be 0 | Should be 0 | Should be 0 | Should be 0 | Should be 0 | 0 |  | Specified in the settings | Specified in the settings |
| Value in DEC | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |  | 0-255 | 0-255 |
| Example in DEC | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 238 | 239 |
| Example in HEX | 0x00 | 0x00 | 0x00 | 0x00 | 0x00 | 0x00 | 0x00 | 0x00 | 0x00 | 0x00 | 0xEE | 0xEF |

----------

### Direct control (Link command 1)
In direct control mode, the values for the roll, pitch, yaw and throttle axes are transmitted to the drone. In fact, with these packets, the drone is controlled as from a remote control.

The payload bytes are in big-endian order (2 bytes per value), then the Link Command byte is equal to 1, then the check-sum and 2 bytes of the end of the packet.

The table below shows the detailed structure of the packet

| Byte N | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Byte name | Payload byte 0 | Payload byte 1 | Payload byte 2 | Payload byte 3 | Payload byte 4 | Payload byte 5 | Payload byte 6 | Payload byte 7 | Link command byte | XOR check-sum | Packet suffix 1 | Packet suffix 2 |
| Description for Direct control | Roll high byte | Roll low byte | Pitch high byte | Pitch low byte | Yaw high byte | Yaw low byte | Throttle high byte | Throttle low byte | 1 |  | Specified in the settings | Specified in the settings |
| Value in DEC | 1000-2000 |  | 1000-2000 |  | 1000-2000 |  | 1000-2000 |  | 1 |  | 0-255 | 0-255 |
| Example in DEC | 1200 |  | 1700 |  | 1500 |  | 1509 |  | 1 | 46 | 238 | 239 |
| Example in HEX | 0x04 | 0xB0 | 0x06 | 0xA4 | 0x05 | 0xDC | 0x05 | 0xE5 | 0x01 | 0x2E | 0xEE | 0xEF |

----------

### Pressure Waypoint (Link command 2)
The pressure waypoint is used at the end of the Liberty-Way sequence when the drone descends over the platform. 4 bytes transmit atmospheric pressure (in Pascals) to the drone.

The payload bytes are in big-endian order (4 bytes for value), then the Link Command byte is equal to 2, then the check-sum and 2 bytes of the end of the packet.

The table below shows the detailed structure of the packet

| Byte N | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Byte name | Payload byte 0 | Payload byte 1 | Payload byte 2 | Payload byte 3 | Payload byte 4 | Payload byte 5 | Payload byte 6 | Payload byte 7 | Link command byte | XOR check-sum | Packet suffix 1 | Packet suffix 2 |
| Description for Pressure waypoint | Pressure 1 byte | Pressure 2 byte | Pressure 3 byte | Pressure 4 byte | Should be 0 | Should be 0 | Should be 0 | Should be 0 | 2 |  | Specified in the settings | Specified in the settings |
| Value in DEC | 1000-120000 |  |  |  | 0 | 0 | 0 | 0 | 2 |  | 0-255 | 0-255 |
| Example in DEC | 101000 |  |  |  | 0 | 0 | 0 | 0 | 2 | 46 | 238 | 239 |
| Example in HEX | 0x00 | 0x01 | 0x8A | 0x88 | 0x00 | 0x00 | 0x00 | 0x00 | 0x02 | 0x01 | 0xEE | 0xEF |

----------

### GPS waypoint (Link command 3)

The GPS waypoint sets the coordinates to which the drone should arrive. Both coordinates (latitude and longitude) are transmitted at once in integer values, 4 bytes for each coordinate (the range is indicated in the table).

The payload bytes are in big-endian order (4 bytes per value), then the Link Command byte is equal to 3, then the check-sum and 2 bytes of the end of the packet.

The table below shows the detailed structure of the packet

| Byte N | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Byte name | Payload byte 0 | Payload byte 1 | Payload byte 2 | Payload byte 3 | Payload byte 4 | Payload byte 5 | Payload byte 6 | Payload byte 7 | Link command byte | XOR check-sum | Packet suffix 1 | Packet suffix 2 |
| Description for GPS waypoint | Latitude 1 byte | Latitude 2 byte | Latitude 3 byte | Latitude 4 byte | Longitude 1 byte | Longitude 2 byte | Longitude 3 byte | Longitude 4 byte | 3 |  | Specified in the settings | Specified in the settings |
| Value in DEC | -90000000-90000000 |  |  |  | -180000000-180000000 |  |  |  | 3 |  | 0-255 | 0-255 |
| Example in DEC | 55588735 |  |  |  | 37627801 |  |  |  | 3 | 208 | 238 | 239 |
| Example in HEX | 0x00 | 0x54 | 0xD2 | 0x5A | 0x00 | 0x39 | 0x6A | 0x5C | 0x03 | 0xD0 | 0xEE | 0xEF |

----------

### Motors stop (Link command 4)

In Motors Stop mode, drone stops action and turns off the motors.

The payload bytes are in big-endian order, then the Link Command byte is equal to 4, then the check-sum and 2 bytes of the end of the packet.

The table below shows the detailed structure of the packet

| Byte N | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Byte name | Payload byte 0 | Payload byte 1 | Payload byte 2 | Payload byte 3 | Payload byte 4 | Payload byte 5 | Payload byte 6 | Payload byte 7 | Link command byte | XOR check-sum | Packet suffix 1 | Packet suffix 2 |
| Description for Motors stop | Should be 0 | Should be 0 | Should be 0 | Should be 0 | Should be 0 | Should be 0 | Should be 0 | Should be 0 | 4 |  | Specified in the settings | Specified in the settings |
| Value in DEC | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 4 |  | 0-255 | 0-255 |
| Example in DEC | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 4 | 4 | 238 | 239 |
| Example in HEX | 0x00 | 0x00 | 0x00 | 0x00 | 0x00 | 0x00 | 0x00 | 0x00 | 0x04 | 0x04 | 0xEE | 0xEF |

----------

### Start Liberty-Way sequence (Link command 5)

In Start Sequene mode, drone starts the Liberty-Way sequence and prepares to take off.

The payload bytes are in big-endian order, then the Link Command byte is equal to 5, then the check-sum and 2 bytes of the end of the packet.

The table below shows the detailed structure of the packet

| Byte N | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Byte name | Payload byte 0 | Payload byte 1 | Payload byte 2 | Payload byte 3 | Payload byte 4 | Payload byte 5 | Payload byte 6 | Payload byte 7 | Link command byte | XOR check-sum | Packet suffix 1 | Packet suffix 2 |
| Description for Start sequence | Should be 0 | Should be 0 | Should be 0 | Should be 0 | Should be 0 | Should be 0 | Should be 0 | Should be 0 | 5 |  | Specified in the settings | Specified in the settings |
| Value in DEC | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 5 |  | 0-255 | 0-255 |
| Example in DEC | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 5 | 5 | 238 | 239 |
| Example in HEX | 0x00 | 0x00 | 0x00 | 0x00 | 0x00 | 0x00 | 0x00 | 0x00 | 0x05 | 0x05 | 0xEE | 0xEF |

----------

### Abort (Link command 6)

In Abort mode, drone clears flags, resets direct corrections, waypoint flags and sharply jumps up to prevent possible collision due to loss of visual contact with the platform.

The payload bytes are in big-endian order, then the Link Command byte is equal to 6, then the check-sum and 2 bytes of the end of the packet.

The table below shows the detailed structure of the packet

| Byte N | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Byte name | Payload byte 0 | Payload byte 1 | Payload byte 2 | Payload byte 3 | Payload byte 4 | Payload byte 5 | Payload byte 6 | Payload byte 7 | Link command byte | XOR check-sum | Packet suffix 1 | Packet suffix 2 |
| Description for Abort | Should be 0 | Should be 0 | Should be 0 | Should be 0 | Should be 0 | Should be 0 | Should be 0 | Should be 0 | 6 |  | Specified in the settings | Specified in the settings |
| Value in DEC | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 6 |  | 0-255 | 0-255 |
| Example in DEC | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 6 | 6 | 238 | 239 |
| Example in HEX | 0x00 | 0x00 | 0x00 | 0x00 | 0x00 | 0x00 | 0x00 | 0x00 | 0x06 | 0x06 | 0xEE | 0xEF |

----------

## TODO

- Make the GPS Mixer a standalone device with its own IP address
- Test the entire system under real conditions
- Add flight to waypoints (waypoints are set by the user)
- Remove barometer from the platform (use only Sonarus on the drone)

----------
