# Liberty-Way AMLS Landing Controller


![Logo](https://github.com/XxOinvizioNxX/Liberty-Way/blob/main/git_images/logo_book.png "Logo")

This project was created as a part of project activities subject of Moscow Polytech University by the 181-311 group.
It also participates in the CopterHack 2021 hackathon from Copter Express.

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
This application is a part of the AMLS project (Autonomous Multi-rotor Landing System).
The task of the project is to automatically land a drone on a platform in motion.

![Screenshot](https://github.com/XxOinvizioNxX/Liberty-Way/blob/main/git_images/Screenshot_1.png "Screenshot")

This application processes the frame received from the camera located on the platform which is looking up. On the bottom of the drone, there is an ARUco tag. The application detects the marker, estimates its position, then passes through the PID controller and sends the correction values to the drone.

------------

### Building and running
Liberty Way is a cross-platform application and has been tested on Linux and Windows. 
You can try running the application on any other operating system. But you first need to build the OpenCV-contrib library (**The releases include libraries for Windows and Linux**).
Builded binary JAR-file can be found in releases as well as in the main branch (Liberty-Way.jar file)


All of the controls is performed only from the browser.
When starting the application, you can specify the arguments:
```
 -c,--color                write colored logs.
 -i,--ip <arg>             server ip
 -sp,--server_port <arg>   web server port (0 - 65535)
 -vp,--video_port <arg>    video stream port (0 - 65535)
```
Also, the server address and ports can be specified in the configuration (settings.json)

-------

###  Configuration
These are the parameters that are set by default and can be changed depending on the application of the program:
```
"marker_size",             size of the tracking marker (cm)
"landing_alt",              altitude of landing allowance
"pid_file",              file of PID configuration
"camera_matrix_file",              camera calibration (matrix)
"camera_distortions_file",     camera calibration (distortions)
"watermark_file",              image of a watermark
"web_resources_folder",              folder of web resources
"web_templates_folder",              folder of templates for a web-page
"frame_width",              resolution of an input frame - width
"frame_height",              resolution of an input frame - height
"disable_auto_exposure",              disabling/enabling auto exposure feature
"disable_auto_wb",              disabling/enabling auto white balance feature
"disable_auto_focus",              disabling/enabling auto focus feature
"default_server_host",              server host which can be overridden by cmd argument
"default_server_port",              server port which can be overridden by cmd argument
"default_video_port",              video port which can be overridden by cmd argument
"video_enabled_by_default",              should the video be enabled from the start
"fps_measure_period",              period of measurements of fps (milliseconds)
"adaptive_thresh_constant",              detector of parameters
"allowed_ids",              array of tracking markers ids
"input_filter",              Kalman filter coefficient
"allowed_lost_frames",              frames where the marker is not in the input frame
"lost_filter",              coefficient of the predicted marker point
"landing_decrement",              constant latitude decrement (cm)
"allowed_landing_range_xy",              range of the landing allowance (cm)
"allowed_landing_range_yaw",              range of the landing allowance (degrees)
"setpoint_x",              absolute value of pid regulation (x)
"setpoint_y",              absolute value of pid regulation (y)
"setpoint_yaw",              absolute value of pid regulation (angle)
"data_suffix_1",          first unique ASCII symbol that shows the end of the package
"data_suffix_2",          second unique ASCII symbol that shows the end of the package
"push_osd_after_frames",           after how many frames the image is being sent to the web-page
```
