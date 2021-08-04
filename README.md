# Liberty-Way AMLS Landing Controller

![Logo](https://github.com/XxOinvizioNxX/Liberty-Way/blob/main/git_images/logo_book.png "Logo")

[![GitHub license](https://img.shields.io/github/license/XxOinvizioNxX/Liberty-Way?color=red)](https://github.com/XxOinvizioNxX/Liberty-Way/blob/main/LICENSE)
[![GitHub issues](https://img.shields.io/github/issues/XxOinvizioNxX/Liberty-Way?color=orange)](https://github.com/XxOinvizioNxX/Liberty-Way/issues)
![GitHub All Releases](https://img.shields.io/github/downloads/XxOinvizioNxX/Liberty-Way/total?color=yellow "GitHub All Releases")
[![Travis (.com)](https://img.shields.io/travis/com/XxOinvizioNxX/Liberty-Way?color=green)](https://www.travis-ci.com/github/XxOinvizioNxX)
[![GitHub stars](https://img.shields.io/github/stars/XxOinvizioNxX/Liberty-Way?color=blue)](https://github.com/XxOinvizioNxX/Liberty-Way/stargazers)
[![Liberty-Way_beta_3.0.0](https://img.shields.io/badge/latest_version-beta_3.0.0-informational?logo=Github&color=purple "Liberty-Way_beta_3.0.0")](https://github.com/XxOinvizioNxX/Liberty-Way/releases/tag/beta_3.0.0 "Liberty-Way_beta_3.0.0")
[![Twitter](https://img.shields.io/twitter/url?style=social&url=https%3A%2F%2Ftwitter.com%2Fliberty_drones)](https://twitter.com/intent/tweet?text=Wow:&url=https%3A%2F%2Ftwitter.com%2Fliberty_drones)

This project is a part of project activity in Moscow Polytech University by students of group 181-311.

Liberty-Way AMLS Landing Controller © 2021 Pavel Neshumov (Fern Hertz)

All the code excluding the dependencies block, was written by Pavel Neshumov (Fern Hertz)

----------

## More projects
### Liberty-X drone flight controller

- **Liberty-X Project** (Flight controller): https://github.com/XxOinvizioNxX/Liberty-X

### Liberty-Way Eitude

- **AMLS platform** (Platform): https://github.com/XxOinvizioNxX/Liberty-Way/tree/main/Eitude

----------

## Feedback

- Pavel Neshumov (Author and CEO of the project) E-Mail: xxoinvizionxx@gmail.com, Twitter: @fern_hertz
- Andrey Kabalin (Project developer. GPS stabilization, hardware development) E-Mail: astik452@gmail.com
- Vladislav Yasnetsky (Project developer. Video editing, PR, hardware development). E-Mail: vlad.yasn@gmail.com

## For project development

- BTC: `bc1qd2j53p9nplxcx4uyrv322t3mg0t93pz6m5lnft`
- ZEC: `t1Jb5tH61zcSTy2QyfsxftUEWHikdSYpPoz`
- ETH: `0x284E6121362ea1C69528eDEdc309fC8b90fA5578`

----------

## Table of contents

- [Dependencies](#dependencies)
- [Description](#description)
- [License](#license)
- [Logotype](#logotype)
- [Building and running](#building-and-running)
- [Configuration](#configuration)
  - [Settings](#settings)
  - [PID](#pid)
- [Data packet structure](#data-packet-structure)

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

----------

## Logotype

AMLS and Liberty-X logo was designed by Pavel Neshumov (Fern Hertz)

----------

## Building and running

Liberty-Way is a cross-platform application and has been tested on Linux and Windows.
You can try running the application on any other operating system. But you first need to build the OpenCV-contrib library (**The releases include libraries for Windows and Linux**).
Builded binary JAR-file can be found in releases.

All of the controls is performed only from the browser.
When starting the application, you can specify the arguments:

```
 -c,--color                write colored logs.
 -i,--ip <arg>             server ip
 -sp,--server_port <arg>   web server port (0 - 65535)
 -vp,--video_port <arg>    video stream port (0 - 65535)
```

Also, the server address and ports can be specified in the configuration (settings.json)

----------

## Configuration

### Settings

These are the parameters presented in settings.json file that are used by the program and can be changed depending of its application:

```
"marker_size",             length of one side of the tracking marker (cm)
"default_exposure",        default exposure value of the camera
"landing_alt",             altitude of sending a packet to turn off the motors (service info = 3)
"pid_file",              file of PID configuration
"camera_matrix_file",              camera calibration (matrix)
"camera_distortions_file",     camera calibration (distortions)
"watermark_file",              image of a watermark (top-right corner)
"web_resources_folder",              folder of web resources
"web_templates_folder",              folder of templates for a web-page
"blackbox_folder",           folder which stores blackboxes' .csv files
"frame_width",              resolution that is set to web-camera (width)
"frame_height",              resolution that is set to web-camera (height)
"disable_auto_exposure",              disabling/enabling auto exposure feature
"disable_auto_wb",              disabling/enabling auto white balance feature
"disable_auto_focus",              disabling/enabling auto focus feature
"default_server_host",              server host which can be overridden by cmd argument
"default_server_port",              server port which can be overridden by cmd argument
"default_video_port",              video port which can be overridden by cmd argument
"video_stream_enabled_by_default",              should the video stream be enabled from the start
"video_on_page_enabled_by_default",              should the video be enabled on the page from the start 
"blackbox_enabled_by_default",                   should the blackbox feature be enabled by default
"platform_light_enable_threshold",              lower threshold of the surrounding light to enable additional lighting
"platform_light_disable_threshold",             upper threshold of the surrounding light to disable additional lighting
"platform_reply_timeout",              amount of milliseconds in which receiving a response from the platform is acceptable
"platform_loop_timer",               update rate of the platform in milliseconds
"fps_measure_period",              period of measurements of fps (milliseconds)
"adaptive_thresh_constant",              detector of parameters (ARUCO)
"aruco_dictionary",             index of used ARUco dictionary (default = 0 which is 50 4x4 marks) 
"allowed_ids",              array of allowed tracking markers ARUCO ids
"input_filter",              Kalman filter coefficient
"setpoint_alignment_factor",        floating setpoint to the desired position coefficient
"allowed_lost_frames",              frames where the marker is not in the input frame
"landing_decrement",              constant latitude decrement (cm)
"allowed_landing_range_xy",              range of the landing allowance (cm)
"allowed_landing_range_yaw",              range of the landing allowance (degrees)
"setpoint_x",              setpoint of PID controller (absolute estimated x)
"setpoint_y",              setpoint of PID controller (absolute estimated y)
"setpoint_yaw",            angle setpoint of PID controller (absolute estimated degrees)
"data_suffix_1",          first of the unique ASCII pair symbols that show the end of the packet
"data_suffix_2",          second of the unique ASCII pair symbols that show the end of the packet
"push_osd_after_frames",           after how many frames the image is being sent to the web-page
```

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

Each packet consists of 12 bytes. The first 8 bytes are the payload. Next comes 1 byte Link Command, which tells the drone what to do with this packet. Then 1 byte XOR of the check-sum of the previous 9 bytes. At the end there are 2 bytes indicating the end of the packet (indicated in three. By default it is 0xEE).

For each packet sent (even IDLE), the drone returns 1 or several bytes (the number is specified in the Liberty-X settings) of telemetry.

All data that is sent to the drone or comes from the drone (telemetry) is arranged in big-endian order.

----------

### IDLE (Link command 0)
In IDLE mode, the drone does not perform any new calculations and simply continues its flight in the previous mode.

The payload bytes must be equal to 0, the Link Command byte is equal to 0, then the check-sum (0) and 2 bytes of the end of the packet.

The table below shows the detailed structure of the packet

| Byte N | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Byte name | Payload byte 0 | Payload byte 1 | Payload byte 2 | Payload byte 3 | Payload byte 4 | Payload byte 5 | Payload byte 6 | Payload byte 7 | Link command byte | XOR check-sum | Packet suffix 1 | Packet suffix 2 |
| Description for IDLE | Should be 0 | Should be 0 | Should be 0 | Should be 0 | Should be 0 | Should be 0 | Should be 0 | Should be 0 | 0 | 0 | Specified in the settings | Specified in the settings |
| Value in DEC | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 238 | 238 |
| Value in HEX | 0x00 | 0x00 | 0x00 | 0x00 | 0x00 | 0x00 | 0x00 | 0x00 | 0x00 | 0x00 | 0xEE | 0xEE |

----------

### Direct control (Link command 1)
In direct control mode, the values for the roll, pitch, yaw and throttle axes are transmitted to the drone. In fact, with these packets, the drone is controlled as from a remote control.

The payload bytes are in big-endian order (2 bytes per value), then the Link Command byte is equal to 1, then the check-sum and 2 bytes of the end of the packet.

The table below shows the detailed structure of the packet

| Byte N | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Byte name | Payload byte 0 | Payload byte 1 | Payload byte 2 | Payload byte 3 | Payload byte 4 | Payload byte 5 | Payload byte 6 | Payload byte 7 | Link command byte | XOR check-sum | Packet suffix 1 | Packet suffix 2 |
| Description for Direct control | Roll high byte | Roll low byte | Pitch high byte | Pitch low byte | Yaw high byte | Yaw low byte | Throttle high byte | Throttle low byte | 1 |  | Specified in the settings | Specified in the settings |
| Value in DEC | 1000-2000 |  | 1000-2000 |  | 1000-2000 |  | 1000-2000 |  | 1 |  | 238 | 238 |
| Example in DEC | 1200 |  | 1700 |  | 1500 |  | 1509 |  | 1 | 46 | 238 | 238 |
| Example in HEX | 0x04 | 0xB0 | 0x06 | 0xA4 | 0x05 | 0xDC | 0x05 | 0xE5 | 0x01 | 0x2E | 0xEE | 0xEE |

----------

If link command == 2 sends pressure waypoint to the drone.

| Byte N         | 0                 | 1                 | 2                  | 3                  | 4              | 5              | 6              | 7              | 8                 | 9              | 10                        | 11                        |
|----------------|-------------------|-------------------|--------------------|--------------------|----------------|----------------|----------------|----------------|-------------------|----------------|---------------------------|---------------------------|
| Byte name      | Payload byte 0    | Payload byte 1    | Payload byte 2     | Payload byte 3     | Payload byte 4 | Payload byte 5 | Payload byte 6 | Payload byte 7 | Link command byte | XOR Check sum  | Pocket suffix 1           | Pocket suffix 2           |
| Description    | pressure low byte | pressure low byte | pressure high byte | pressure high byte | -              | -              | -              | -              |                   |                | Specified in the settings | Specified in the settings |
| Value in DEC   | 101,000           |                   |                    |                    |                |                |                |                |                   |                | 238                       | 238                       |
| Example in HEX | 0x00              | 0x01              | 0x8A               | 0x88               | 0x00           | 0x00           | 0x00           | 0x00           | 0x02              | 0x01           | 0xEE                      | 0xEE                      |
| Example in DEC | 0                 | 1                 | 138                | 136                | 0              | 0              | 0              | 0              | 2                 | 1              | 238                       | 238                       |

----------

If link command == 3 sends gps waypoint to the drone by latitude and longitude.

| Byte N         	|        -->         	| 0         	| 1     	| 2     	| 3     	| 4        	| 5     	| 6     	| 7     	| 8                    	| 9               	| 10            	| 11            	|
|----------------	|--------------------	|-----------	|-------	|-------	|-------	|----------	|-------	|-------	|-------	|----------------------	|-----------------	|---------------	|---------------	|
| Name           	| sendGPSWaypoint    	| Lat 1     	| Lat 2 	| Lat 3 	| Lat 4 	| Lon 1    	| Lon 2 	| Lon 3 	| Lon 4 	| Link command byte    	| Check byte      	| Pocket end    	| Pocket end    	|
| Value          	|        -->         	| 55588735  	|       	|       	|       	| 37627801 	|       	|       	|       	| 3                    	| 208             	|               	|               	|
| Description    	| Sends gps waypoint 	|           	|       	|       	|       	|          	|       	|       	|       	| Current command vlue 	| Bytes value XOR 	| Data suffix 1 	| Data suffix 2 	|
| Example in HEX 	|        -->         	| 0x00      	| 0x54  	| 0xD2  	| 0x5A  	| 0x00     	| 0x39  	| 0x6A  	| 0x5C  	| 0x03                 	| 0xD0            	| 0xEE          	| 0xEE          	|

----------

If link command == 4 sends a command to turn off the motors.

| Byte N         	|        -->           	| 0    	| 1    	| 2    	| 3    	| 4    	| 5    	| 6    	| 7    	| 8                    	| 9               	| 10            	| 11            	|
|----------------	|----------------------	|------	|------	|------	|------	|------	|------	|------	|------	|----------------------	|-----------------	|---------------	|---------------	|
| Name           	| sendMotorsStop       	|      	|      	|      	|      	|      	|      	|      	|      	| Link command byte    	| Check byte      	| Pocket end    	| Pocket end    	|
| Value          	|        -->           	| 0    	| 0    	| 0    	| 0    	| 0    	| 0    	| 0    	| 0    	| 4                    	| 4               	|               	|               	|
| Description    	| turns off the motors 	|      	|      	|      	|      	|      	|      	|      	|      	| Current command vlue 	| Bytes value XOR 	| Data suffix 1 	| Data suffix 2 	|
| Example in HEX 	|        -->           	| 0x00 	| 0x00 	| 0x00 	| 0x00 	| 0x00 	| 0x00 	| 0x00 	| 0x00 	| 0x04                 	| 0x4             	| 0xEE          	| 0xEE          	|

----------

If link command == 5 sends a command to turn off the motors and sends takeoff command.

| Byte N         	|        -->        	| 0    	| 1    	| 2    	| 3    	| 4    	| 5    	| 6    	| 7    	| 8                    	| 9               	| 10            	| 11            	|
|----------------	|-------------------	|------	|------	|------	|------	|------	|------	|------	|------	|----------------------	|-----------------	|---------------	|---------------	|
| Name           	| sendStartSequence 	|      	|      	|      	|      	|      	|      	|      	|      	| Link command byte    	| Check byte      	| Pocket end    	| Pocket end    	|
| Value          	|        -->        	| 0    	| 0    	| 0    	| 0    	| 0    	| 0    	| 0    	| 0    	| 5                    	| 5               	|               	|               	|
| Description    	| Sends takeoff     	|      	|      	|      	|      	|      	|      	|      	|      	| Current command vlue 	| Bytes value XOR 	| Data suffix 1 	| Data suffix 2 	|
| Example in HEX 	|        -->        	| 0x00 	| 0x00 	| 0x00 	| 0x5A 	| 0x00 	| 0x00 	| 0x00 	| 0x00 	| 0x05                 	| 0x5             	| 0xEE          	| 0xEE          	|

----------

If link command == 6 Sends abort command to the drone clears flags, resets direct corrections, waypoint flags and sharply jumps up to prevent a collision.

| Byte N         	|        -->  	| 0    	| 1    	| 2    	| 3    	| 4    	| 5    	| 6    	| 7    	| 8                    	| 9               	| 10            	| 11            	|
|----------------	|-------------	|------	|------	|------	|------	|------	|------	|------	|------	|----------------------	|-----------------	|---------------	|---------------	|
| Name           	| sendAbort   	|      	|      	|      	|      	|      	|      	|      	|      	| Link command byte    	| Check byte      	| Pocket end    	| Pocket end    	|
| Value          	|        -->  	| 0    	| 0    	| 0    	| 0    	| 0    	| 0    	| 0    	| 0    	| 6                    	| 6               	|               	|               	|
| Description    	| Sends abort 	|      	|      	|      	|      	|      	|      	|      	|      	| Current command vlue 	| Bytes value XOR 	| Data suffix 1 	| Data suffix 2 	|
| Example in HEX 	|        -->  	| 0x00 	| 0x00 	| 0x00 	| 0x5A 	| 0x00 	| 0x00 	| 0x00 	| 0x00 	| 0x06                 	| 0x6             	| 0xEE          	| 0xEE          	|

----------

## TODO

- We will switch from bing maps to street maps due to street maps accessibility
- Communication between the platform and Liberty-X will change
- Possibility to communication through the ethernet port
- Lux meter will be added to the main firmware

----------
