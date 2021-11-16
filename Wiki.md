# Liberty Drones - EN

![Logo](https://github.com/XxOinvizioNxX/Liberty-Way/blob/main/git_images/logo_book.png "Logo")

## The goal of our project is to create a system for autonomous control, tracking and landing of drones as a part of a bigger delivery system. 
Our system is capable of guiding the drone towards GPS waypoints, stabilizing it over the platform using GPS and optical stabilizations, and performing capture and landing.
### Liberty Drones Article

This Article describes the Libery Drones project. Namely,Liberty-X, Eitude, Sonarus, GPS Mixer and GPS to Serial app.

### Our main GitHub repository

https://github.com/XxOinvizioNxX/Liberty-Way

### Developers

- [Pavel Neshumov](mailto:xxoinvizionxx@gmail.com)
- [Andrey Kabalin](mailto:astik452@gmail.com)
- [Vladislav Yasnetsky](mailto:vlad.yasn@gmail.com)

![Drone 1](https://github.com/XxOinvizioNxX/Liberty-Way/blob/main/git_images/drone_meme.jpg "Drone 1")

-----------

## Table of contents

- [0. Liberty Drones projects](#liberty-drones-project)
  - [Liberty-X](#liberty-x)
  - [Eitude](#eitude)
  - [Sonarus](#sonarus)
  - [GPS Mixer](#gps-mixer)
  - [GPS to Serial](#gps-to-serial)
- [1. Liberty-X](#liberty-x)
- [1. GPS hold and Flight to waypoints functions](#1-gps-hold-and-flight-to-waypoints-functions)
  - [1.1. Serial reading](#11-serial-reading)
  - [1.2. UBlox GPS parsing](#12-ublox-gps-parsing)
  - [1.3. Set current waypoint](#13-set-current-waypoint)
  - [1.4. Waypoint edition (To fly to waypoints)](#14-waypoint-edit-to-fly-to-waypoints)
  - [1.5. Waypoint stabilization](#15-waypoint-stabilization)
- [2. GPS following](#2-gps-following)
- [3. Compass](#3-compass)
- [4. Altitude stabilization (barometer)](#4-altitude-stabilization-barometer)
- [5. Optical stabilization](#5-optical-stabilization)
  - [5.1. So difficult and so important](#51-so-difficult-and-so-important)
  - [5.2. First steps](#52-first-steps)
  - [5.3. Inverse approach](#53-inverse-approach)
  - [5.4. Java edition](#54-java-edition)
  - [5.5. Liberty-Way](#55-liberty-way)
  - [5.6. Communication with the drone](#56-communication-with-the-drone)
  - [5.7. Camera gimbal](#57-camera-gimbal)
- [6. Eitude AMLS Platform](#6-eitude-amls-platform)
  - [6.1. Grabbing system](#61-grabbing-system)
  - [6.2. Weather protection system](#62-weather-protection-system)
  - [6.3. Speed measurement system](#63-platform-speedometer)
  - [6.4. Illumination system](#64-platform-light-sensor)
- [7. Conclusion](#7-conclusion)

-----------

## 0. What is Liberty Drones?

----------

The goal of our project is to create a system for autonomous control, tracking and landing of drones as a part of a bigger delivery system. Our system is capable of guiding the drone towards GPS waypoints, stabilizing it over the platform using GPS and optical stabilizations, and performing capture and landing.

### Liberty-X

Drone flight controller firmware for STM32 microcontroller. This flight controller is suitable for camera drones, scientific drones or delivery drones (as part of the Liberty Drones project) The Liberty project was created as a part of project activities subject of Moscow Polytech University by the 181-311 group This project is a fork of the YMFC-32 project. The project is under development. Description, PCB files, schematics, OSD, etc. coming soon

- https://github.com/XxOinvizioNxX/Liberty-X

### Eitude

Liberty Drones Platform controller This is a fully functional and reliable part of the Liberty Drones system from the platform perspective. Controller will be operating with such data as its own GPS location, level of illumination around it, its speed and the data that would be sent from the drone which is descripted in Data packet structure paragraph in Liberty-Way Project. So far, this system is able to measure the level of illumination of the surroundings, the speed of the platform, and also turn on/off the backlight.

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
How the system operates:

- Firstly, a drone with a delivery package is far from the platform and it has no visual contact with it. The drone recieves GPS coordinates of a platform by using cellular communication or any other radio channel (The drone has Liberty-Link implemented on it. This module is able to adjust its position, whatever the firmware of the flight controller. The module is installed inside the line between a receiver and a flight controller.
- The drone is moving to received coordinates. The coordinates might be renewed in the process (but not frequently, thus preventing the channel from overloading)
- When the drone is close to the platform but there is still no visual contact, the program runs GPS stabilization. Here the data is being transmitted over the closest radio communication channel of high freqency, so the drone can catch up with the platform.
- Meanwhile, the drone descends (barometers are installed on both, the drone and the platform). Descending goes on untill altitude reaches 1.5-2 meters above the platform.
- While descending and when visual contact with the platform camera is established, the program enables visual (precision) stabilization. And as soon as the drone's tag is within camera's field of view, the algorithm will capture the drone.
- When optical stabilization is enabled, GPS is working as a back up plan (in case something goes wrong, GPS stabilization launches again).
- In order to use optical stabilization the drone is equipped with ArUco tag which can be captured by a camera and by using the closest radio communication channel, the system transmits adjustment data to the drone.
- Along with optical stabilization, the program launches landing algorithm. The algorithm artificially and smoothly reduces the setpoint of height (Z) until it reaches a certain threshold.
- When the drone is approaching on the desirable height, the program enables grabbing system implemented on the platform. Those grips are used to catch and hold the drone in the process of landing and after the drone was caught.
- When the landing is completed, the platform starts maintenance work and in order to protect the drone frome external influences, the program enables weather protection and closes the roof above landing area.
- Landing accomplished!  

## 1. GPS hold and Flight to waypoints functions

As stated earlier, the drone is equipped with "universal" module Liberty-Link, which is receiving commands from the platform and adjusting the drone's position by interfering into the remote control signal (More in the following paragraphs).

GPS module will be built in Liberty-Link, so it would have the ability to maintain the drone's GPS position and follow GPS points.

GPS-module will be used from the UBlox group (for instance, UBlox Neo-M8). There will be 1 or 3 (to minimize the error) modules.

![GPS Module](https://github.com/XxOinvizioNxX/Liberty-Way/blob/main/git_images/liberty-x_front_cutout_2_small_gps.png "GPS Module")

Modules operate via UART, configured to send data 5 times per second. The Liberty-Link firmware will read data from the modules and calculate the coordinates of the current position.

### 1.1. Serial reading

Reading data from a module into a buffer looks like this:

```cpp
// Read data from the GPS module
while (GPS_serial.available() && new_line_found == 0) {
	// Stay in this loop as long as there is serial information from the GPS available
	char read_serial_byte = GPS_serial.read();
	if (read_serial_byte == '$') {
		// Clear the old data from the incoming buffer array if the new byte equals a $ character
		for (message_counter = 0; message_counter <= 99; message_counter++) {
			incoming_message[message_counter] = '-';
		}
		// Reset the message_counter variable because we want to start writing at the begin of the array
		message_counter = 0;
	}
	// If the received byte does not equal a $ character, increase the message_counter variable
	else if (message_counter <= 99)
		message_counter++;
	
	// Write the new received byte to the new position in the incoming_message array
	incoming_message[message_counter] = read_serial_byte;

	// Every NMEA line ends with a '*'. If this character is detected the new_line_found variable is set to 1
	if (read_serial_byte == '*') new_line_found = 1;
}
```

### 1.2. UBlox GPS parsing

After that, latitude, longitude, a type of correction (2D, 3D) and the number of satellites are calculated from the filled buffer.
Parsing GPS data of the UBlox protocol looks like this:

```cpp
// If the software has detected a new NMEA line it will check if it's a valid line that can be used
if (new_line_found == 1) {
	// Reset the new_line_found variable for the next line
	new_line_found = 0;
	if (incoming_message[4] == 'L' && incoming_message[5] == 'L' && incoming_message[7] == ',') {
		// When there is no GPS fix or latitude/longitude information available
		// Set some variables to 0 if no valid information is found by the GPS module. This is needed for the GPS loss when flying
		l_lat_gps = 0;
		l_lon_gps = 0;
		lat_gps_previous = 0;
		lon_gps_previous = 0;
		number_used_sats = 0;
	}
	// If the line starts with GA and if there is a GPS fix we can scan the line for the latitude, longitude and number of satellites
	if (incoming_message[4] == 'G' && incoming_message[5] == 'A' && (incoming_message[44] == '1' || incoming_message[44] == '2')) {
		// Filter the minutes for the GGA line multiplied by 10
		lat_gps_actual = ((int)incoming_message[19] - 48) * (long)10000000;
		lat_gps_actual += ((int)incoming_message[20] - 48) * (long)1000000;
		lat_gps_actual += ((int)incoming_message[22] - 48) * (long)100000;
		lat_gps_actual += ((int)incoming_message[23] - 48) * (long)10000;
		lat_gps_actual += ((int)incoming_message[24] - 48) * (long)1000;
		lat_gps_actual += ((int)incoming_message[25] - 48) * (long)100;
		lat_gps_actual += ((int)incoming_message[26] - 48) * (long)10;
		// To convert minutes to degrees we need to divide minutes by 6
		lat_gps_actual /= (long)6;
		// Add multiply degrees by 10
		lat_gps_actual += ((int)incoming_message[17] - 48) * (long)100000000;
		lat_gps_actual += ((int)incoming_message[18] - 48) * (long)10000000;
		// Divide everything by 10
		lat_gps_actual /= 10;

		// Filter minutes for the GGA line multiplied by 10
		lon_gps_actual = ((int)incoming_message[33] - 48) * (long)10000000; 
		lon_gps_actual += ((int)incoming_message[34] - 48) * (long)1000000;
		lon_gps_actual += ((int)incoming_message[36] - 48) * (long)100000;
		lon_gps_actual += ((int)incoming_message[37] - 48) * (long)10000;
		lon_gps_actual += ((int)incoming_message[38] - 48) * (long)1000;
		lon_gps_actual += ((int)incoming_message[39] - 48) * (long)100;
		lon_gps_actual += ((int)incoming_message[40] - 48) * (long)10;
		// To convert minutes to degrees we need to divide minutes by 6
		lon_gps_actual /= (long)6;
		// Add multiply degrees by 10
		lon_gps_actual += ((int)incoming_message[30] - 48) * (long)1000000000;
		lon_gps_actual += ((int)incoming_message[31] - 48) * (long)100000000;
		lon_gps_actual += ((int)incoming_message[32] - 48) * (long)10000000;
		// Divide everything by 10
		lon_gps_actual /= 10;

		if (incoming_message[28] == 'N')
			// When flying north of the equator the latitude_north variable will be set to 1
			latitude_north = 1;
		else
			// When flying south of the equator the latitude_north variable will be set to 0
			latitude_north = 0;

		if (incoming_message[42] == 'E')
			// When flying east of the prime meridian the longiude_east variable will be set to 1
			longiude_east = 1;
		else
			// When flying west of the prime meridian the longiude_east variable will be set to 0
			longiude_east = 0;
			
		// Filter the number of satillites from the GGA line
		number_used_sats = ((int)incoming_message[46] - 48) * (long)10;
		number_used_sats += (int)incoming_message[47] - 48;

		if (lat_gps_previous == 0 && lon_gps_previous == 0) {
			// If this is the first time the GPS code is used
			// Set the lat_gps_previous variable to the lat_gps_actual variable
			lat_gps_previous = lat_gps_actual;
			// Set the lon_gps_previous variable to the lon_gps_actual variable
			lon_gps_previous = lon_gps_actual;
		}

		// Divide the difference between the new and the previous latitudes by 10
		lat_gps_loop_add = (float)(lat_gps_actual - lat_gps_previous) / 10.0;
		// Divide the difference between the new and the previous longitudes by 10
		lon_gps_loop_add = (float)(lon_gps_actual - lon_gps_previous) / 10.0;

		// Set the l_lat_gps variable to the previous latitude value
		l_lat_gps = lat_gps_previous;
		// Set the l_lon_gps variable to the previous longitude value
		l_lon_gps = lon_gps_previous;

		// Remember the new latitude value in the lat_gps_previous variable for the next loop
		lat_gps_previous = lat_gps_actual;
		// Remember the new longitude value in the lat_gps_previous variable for the next loop
		lon_gps_previous = lon_gps_actual;
	}

	// If the line starts with SA and if there is a GPS fix we can scan the line for the fix type (none, 2D or 3D)
	if (incoming_message[4] == 'S' && incoming_message[5] == 'A')
		fix_type = (int)incoming_message[9] - 48;

}
```

### 1.3. Set current waypoint

When required data is received the main magic happens. To enable maintaining of the current position it will be enough to set the flag `waypoint_set = 1;` and set current coordinates as a waypoint:

```cpp
l_lat_waypoint = l_lat_gps;
l_lon_waypoint = l_lon_gps;
```

After that, the calculation of the error in the coordinates will begin, correction works with the help of a PD - regulator. For D - component we use rotation memory.

### 1.4. Waypoint edit (To fly to waypoints)

If you just set the new `l_lat_waypoint` and `l_lon_wayoint`, which are located at a great distance from the drone, the drone will not be able to fly normally and stabilize at these coordinates. For smooth adjustments `l_lat_gps_float_adjust` and `l_lon_gps_float_adjust` can be used. These are `float` variables, changing which will smoothly shift `l_lat_waypoint` and `l_lon_waypoint`.

For example, if in the main loop you will constantly add a certain value to these variables:

```cpp
l_lat_gps_float_adjust += 0.0015;
```

With set waypoint, the drone will move smoothly in the given direction.
In the future, this will be used for the smooth drone's acceleration and deceleration while moving to its destination.

### 1.5. Waypoint stabilization

```cpp
if (waypoint_set == 1) {                           
	//If the waypoints are stored
	
	// Adjust l_lat_waypoint
	if (l_lat_gps_float_adjust > 1) {
		l_lat_waypoint++;
		l_lat_gps_float_adjust--;
	}
	if (l_lat_gps_float_adjust < -1) {
		l_lat_waypoint--;
		l_lat_gps_float_adjust++;
	}

	// Adjust l_lon_waypoint
	if (l_lon_gps_float_adjust > 1) {
		l_lon_waypoint++;
		l_lon_gps_float_adjust--;
	}
	if (l_lon_gps_float_adjust < -1) {
		l_lon_waypoint--;
		l_lon_gps_float_adjust++;
	}

	// Calculate the latitude error between waypoint and actual position
	gps_lon_error = l_lon_waypoint - l_lon_gps;
	// Calculate the longitude error between waypoint and actual position
	gps_lat_error = l_lat_gps - l_lat_waypoint;

	// Subtract the current memory position to make room for the new value
	gps_lat_total_avarage -= gps_lat_rotating_mem[gps_rotating_mem_location];
	// Calculate the new change between the actual pressure and the previous measurements
	gps_lat_rotating_mem[gps_rotating_mem_location] = gps_lat_error - gps_lat_error_previous;
	// Add the new value to the long term average value
	gps_lat_total_avarage += gps_lat_rotating_mem[gps_rotating_mem_location];

	// Subtract the current memory position to make room for the new value
	gps_lon_total_avarage -= gps_lon_rotating_mem[gps_rotating_mem_location];
	// Calculate the new change between the actual pressure and the previous measurements
	gps_lon_rotating_mem[gps_rotating_mem_location] = gps_lon_error - gps_lon_error_previous;
	// Add the new value to the long term avarage value
	gps_lon_total_avarage += gps_lon_rotating_mem[gps_rotating_mem_location];
	
	// Increase the rotating memory location
	gps_rotating_mem_location++;
	if (gps_rotating_mem_location == 35)
		// Start at 0 when the memory location 35 is reached
		gps_rotating_mem_location = 0;

	// Remember the error for the next loop
	gps_lat_error_previous = gps_lat_error;
	gps_lon_error_previous = gps_lon_error;

	//Calculate the GPS pitch and roll corrections as if the nose of the multicopter is facing north.
	//The Proportional part = (float)gps_lat_error * gps_p_gain.
	//The Derivative part = (float)gps_lat_total_avarage * gps_d_gain.
	gps_pitch_adjust_north = (float)gps_lat_error * gps_p_gain + (float)gps_lat_total_avarage * gps_d_gain;
	gps_roll_adjust_north = (float)gps_lon_error * gps_p_gain + (float)gps_lon_total_avarage * gps_d_gain;

	if (!latitude_north)
		// Invert the pitch adjustmet because the quadcopter is flying south of the equator
		gps_pitch_adjust_north *= -1;
	if (!longiude_east)
		// Invert the roll adjustmet because the quadcopter is flying west of the prime meridian
		gps_roll_adjust_north *= -1;

	//Because the correction is calculated as if the nose was facing north, we need to convert it for the current heading.
	gps_roll_adjust = ((float)gps_roll_adjust_north * cos(angle_yaw * 0.017453)) + ((float)gps_pitch_adjust_north * cos((angle_yaw - 90) * 0.017453));
	gps_pitch_adjust = ((float)gps_pitch_adjust_north * cos(angle_yaw * 0.017453)) + ((float)gps_roll_adjust_north * cos((angle_yaw + 90) * 0.017453));

	//Limit the maximum correction to 300. This way we still have the full control of the drone with the pitch and roll sticks on the transmitter.
	if (gps_roll_adjust > 300) gps_roll_adjust = 300;
	if (gps_roll_adjust < -300) gps_roll_adjust = -300;
	if (gps_pitch_adjust > 300) gps_pitch_adjust = 300;
	if (gps_pitch_adjust < -300) gps_pitch_adjust = -300;
}
```

## 2. GPS following

The main part of stabilization using GPS coordinates was the development of an algorithm for predicting the position of the drone. The simplest idea was to use a mathematical calculation of the next drone's position. This is calculated for the most accurate positioning in relation to the landing platform.

At the beginning we developed a simple algorithm for calculating the coefficient of coordinates' change. The development was done using Python. At the stage of testing this algorithm, the problem of simulating the generation of GPS coordinates arose. To solve this problem, many different resources were used: from open source homemade navigators to trying to use the Google Maps, Yandex Maps or 2GIS APIs.
And only after 3 months, we thought of a simple soulution: to change the values with some delta and to visualize using MatPlotLib or PyQtGraph.
Prior to this, all testing of the algorithm was carried out using the PX4 firmware toolkit and the Gazebo drone motion simulator. As a result, many formalities were overcome in terms of communicating with the simulator and increasing performance (click on the picture to see the video).

The result of the GPS prediction (clickable):
[![Watch the video](https://github.com/XxOinvizioNxX/Liberty-Way/blob/main/git_images/youtube_gazebo.jpg)](https://youtu.be/Rg-Y_fl4BKQ)

The end result reached a point when the error of the predicted postition varies from 0 to 70 centimeters.

-----------

## 3. Compass

Before optical stabilization launches (during GPS stabilization process), to calculate the GPS correction vector, you need to know the exact angle from the compass. For this, a compass built into the GPS module is used.

Because during the flight, the roll and pitch angles change and a user needs to correct the values from the compass.
In general, calculating the angle from the compass looks like this:

```cpp
// The compass values change when the roll and pitch angles of the quadcopter change. That's the reason why the x and y values need to be calculated for a virtual horizontal position
// The 0.0174533 value is phi/180 as the functions are in radians in stead of degrees
compass_x_horizontal = (float)compass_x * cos(angle_pitch * -0.0174533) + (float)compass_y * sin(angle_roll * 0.0174533) * sin(angle_pitch * -0.0174533) - (float)compass_z * cos(angle_roll * 0.0174533) * sin(angle_pitch * -0.0174533);
compass_y_horizontal = (float)compass_y * cos(angle_roll * 0.0174533) + (float)compass_z * sin(angle_roll * 0.0174533);

// Now that the horizontal values are known the heading can be calculated. With the following lines of code the heading is calculated in degrees.
// Please note that the atan2 uses radians in stead of degrees. That is why the 180/3.14 is used.
if (compass_y_horizontal < 0)actual_compass_heading = 180 + (180 + ((atan2(compass_y_horizontal, compass_x_horizontal)) * (180 / 3.14)));
else actual_compass_heading = (atan2(compass_y_horizontal, compass_x_horizontal)) * (180 / 3.14);

// Add the declination to the magnetic compass heading to get the geographic north
actual_compass_heading += declination;
// If the compass heading becomes smaller than 0, 360 is added to keep it in the 0-360 degrees range
if (actual_compass_heading < 0) actual_compass_heading += 360;
// If the compass heading becomes larger then 360, 360 is subtracted to keep it in the 0-360 degrees range
else if (actual_compass_heading >= 360) actual_compass_heading -= 360;
```

It is clear that the angle from the compass can also be used to maintain the yaw angle of the drone. With point-to-point flights, this may be realized. But at the moment, there is no urgent need for this, because after the start of optical stabilization, the algorithm is able to correct the drone regardless of its yaw angle. Also, during optical stabilization, the yaw angle is automatically corrected.

-----------

## 4. Altitude stabilization (barometer)

Before optical stabilization launches (during GPS stabilization process), our Liberty-Link module will be able to maintain altitude using a barometer.

The platform, as well as the Liberty-Link, will have MS5611 barometers.

![MS5611](https://github.com/XxOinvizioNxX/Liberty-Way/blob/main/git_images/ms5611_barometer.png "MS5611")

According to the documentation, the height resolution is 10 cm. The algorithm will take the pressure values and by passing it through the PID-controller will stabilize the drone's altitude by changing the Throttle (3rd channel).

Altitude hold test (clickable):
[![Watch the video](https://github.com/XxOinvizioNxX/Liberty-Way/blob/main/git_images/youtube_pressure_holding.jpg)](https://youtu.be/xmvcGeZzEfc)

During the flight along the waypoint, the setpoint of the pressure will decrease in order to increase the altitude (it is safer to fly in a straight line at a high altitude, so the drone would not crash into anything). And during GPS stabilization (when the drone is already close to the platform), the drone will be set with a setpoint of pressure that correlates with ~ 1.5-2m height above the platform.

-----------

## 5. Optical stabilization

### 5.1. So difficult and so important

Optical stabilization is the most important and challenging part of our project. In the current condition it is possible to keep the drone above the platform still only using these algorithms. The current version of the OIS algorithm, along with a description of the usage and making, is available in our main GitHub repository. In the future, GPS stabilization will be added to it.

### 5.2. First steps

And as we couldn't predict the possibility of accomplishing our task, first of all, we started to think about the solution for the stabilization system. Afterwards, we settled with the stabilization using augmented reality tags. Firstly, it won't take much finances as we do not need GPS or RTK systems and it will be accurate enough to accomplish its purpose.
Our first idea was to attach Raspberry Pi with Liberty_X as it was embodied in COEX Clover and to let Raspberry Pi handle all of the maths.

First optical stabilization prototype test (Сlickable):
[![Watch the video](https://github.com/XxOinvizioNxX/Liberty-Way/blob/main/git_images/youtube_first_tests.jpg)](https://youtu.be/TrrxXOHAqbQ)

But few tests showed that Raspberry Pi computing power is not enough for amount of data needed to stabilize the drone. Furthermore, the idea of installing a Raspberry Pi on each drone is irrational for its own.

Also, we had middling prototypes, for example, attempts to use color markers (circles of different colors), but these ideas did not work well enough.

### 5.3. Inverse approach

Then we came up with the idea of separating drone and stabilization system so the main math will be accomplished on the landing platform with powerful machine.

This was how we ended up with our current optical stabilization algorithm - the camera which is connected to a powerful machine and the machine is attached to the platform. The drone only has 4x4 ArUco tag and its controller.

(clickable)
[![Watch the video](https://github.com/XxOinvizioNxX/Liberty-Way/blob/main/git_images/youtube_stabilization_1.jpg)](https://youtu.be/A2oq6zCebVo)

Then, we came up with using Pose Estimation algorithms from OpenCV library. The first tests showed us that we are on the right track!

Pose Estimation Pyhton (Clickable):
[![Watch the video](https://github.com/XxOinvizioNxX/Liberty-Way/blob/main/git_images/youtube_stabilization_2.jpg)](https://www.youtube.com/watch?v=kE3UmJZ00so)

But, the algorithms were far from perfect. For example, since the code was written in Python (https://github.com/XxOinvizioNxX/Liberty-X_Point), the performance was not satisfyingly, and there was no suitable threading control either. Therefore, we had to change something.

### 5.4. Java edition

Having weighed all the pros and cons, it was decided to rewrite all optical stabilization using Java. This is how the first version of Liberty-Way appeared. This time, it was decided to approach the OOP thoroughly, and, after a little tweaking, an excellent stabilization and landing algorithm was obtained.

Landing test on Liberty-Way v.beta_0.0.1 (clickable):
[![Watch the video](https://github.com/XxOinvizioNxX/Liberty-Way/blob/main/git_images/youtube_landing_test.jpg)](https://youtu.be/8VAobWPFG8g)

### 5.5. Liberty-Way

Then many improvements and bug fixes followed. As a result, Liberty-Way is a cross-platform web sarvar application that is very convenient for configuration and debugging. Also, in the latest versions (beta_1.0.3 - beta_1.1.2) a blackbox feature was introduced (for recording logs), as well as communication with the platform and many other necessary algorithms.

Full description, including all settings, startup, etc. you can find in our GitHub repository: https://github.com/XxOinvizioNxX/Liberty-Way

Video of static stabilization (clickable):
[![Watch the video](https://github.com/XxOinvizioNxX/Liberty-Way/blob/main/git_images/youtube_static_stabilization.jpg)](https://www.youtube.com/watch?v=adR38R27MEU&ab_channel=AMLSMosPolytech)

Liberty-Way can even stabilize a "thrown" drone (clickable):
[![Watch the video](https://github.com/XxOinvizioNxX/Liberty-Way/blob/main/git_images/youtube_optical_catch.jpg)](https://www.youtube.com/watch?v=gAaGQSC-r2g&ab_channel=AMLSMosPolytech)

There is a small bug in the video with the rotation angle, in the new release it has been fixed!

And, of course, example of how it works in motion (tested with beta_0.0.3 release) (clickable):

[![Watch the video](https://github.com/XxOinvizioNxX/Liberty-Way/blob/main/git_images/youtube_holding_in_motion.jpg)](https://www.youtube.com/watch?v=8vB-8QIBoJU&ab_channel=AMLSMosPolytech)

All basic settings are conveniently placed in separate JSON files (settings, PID), which allows a user to quickly change the required parameters without rebuilding the application. In fact, to run the application, you just need to download the latest release, unpack the archive and run it through the launcher corresponding to the preferable OS.

### 5.6. Communication with the drone

The Liberty-Way connects to the Liberty-Link module installed on the drone and adjusts its position by directly controlling four main channels of the remote control. In one cycle (each frame from the camera), 12 bytes of correction data are sent to the module:
![Packet](https://github.com/XxOinvizioNxX/Liberty-Way/blob/main/git_images/data_structure.png "Data packet")

Bytes description:

- **Roll bytes** - Roll correction values
- **Pitch bytes** - Pitch correction values
- **Yaw bytes** - Yaw correction values
- **Altitude bytes** - Altitude correction values
- **Service info** - sets the drone state (0 - Nothing to do, 1 - Stabilization, 2 - Landing (command not implemented and will be removed in the future. This is not a real landing, just to tell the drone to start decreasing altitude), 3 - Disable motors)
- **Check byte** - XOR sum of all previous bytes that is compared via transmittion in order to verify the data
- **Data suffix** - unique pair of ASCII symbols that is not represented in the packet in any form and that shows the end of the packet

On the drone side (Liberty-Link module), data reading is performing as follows:

```cpp
while (Telemetry_serial.available()) {
	tdc_receive_buffer[tdc_receive_buffer_counter] = Telemetry_serial.read();
	if (tdc_receive_byte_previous == 'L' && tdc_receive_buffer[tdc_receive_buffer_counter] == 'X') {
		tdc_receive_buffer_counter = 0;
		if (tdc_receive_start_detect >= 2) {
			tdc_check_byte = 0;
			for (tdc_temp_byte = 0; tdc_temp_byte <= 8; tdc_temp_byte++)
				tdc_check_byte ^= tdc_receive_buffer[tdc_temp_byte];
			if (tdc_check_byte == tdc_receive_buffer[9]) {
				direct_roll_control = (uint32_t)tdc_receive_buffer[1] | (uint32_t)tdc_receive_buffer[0] << 8;
				direct_pitch_control = (uint32_t)tdc_receive_buffer[3] | (uint32_t)tdc_receive_buffer[2] << 8;
				direct_yaw_control = (uint32_t)tdc_receive_buffer[5] | (uint32_t)tdc_receive_buffer[4] << 8;
				direct_throttle_control = (uint32_t)tdc_receive_buffer[7] | (uint32_t)tdc_receive_buffer[6] << 8;
				direct_service_info = (uint32_t)tdc_receive_buffer[8];

				if (direct_roll_control > 1100 && direct_roll_control < 1900 &&
					direct_pitch_control > 1100 && direct_pitch_control < 1900 &&
					direct_yaw_control > 1100 && direct_yaw_control < 1900 &&
					direct_throttle_control > 1100 && direct_throttle_control < 1900 &&
					/*flight_mode == 2 &&*/ channel_7 > 1500) {
					tdc_timer = millis();
					tdc_working = 1;
				}
				else
					tdc_working = 0;
			}
			else {
				direct_roll_control = 1500;
				direct_pitch_control = 1500;
				tdc_working = 0;
			}
		} else
			tdc_receive_start_detect++;
	}
	else {
		tdc_receive_byte_previous = tdc_receive_buffer[tdc_receive_buffer_counter];
		tdc_receive_buffer_counter++;
		if (tdc_receive_buffer_counter > 11)tdc_receive_buffer_counter = 0;
	}
}
if (millis() - tdc_timer >= 500) {
	tdc_working = 0;
}
if (tdc_working && direct_service_info == 2 && !return_to_home_step)
	return_to_home_step = 3;
if (!tdc_working)
	return_to_home_step = 0;
if (!tdc_working || direct_service_info < 1) {
	direct_roll_control = 1500;
	direct_pitch_control = 1500;
	direct_yaw_control = 1500;
	direct_throttle_control = 1500;
}
```

As a result, there are 4 variables:

```
direct_roll_control
direct_pitch_control
direct_yaw_control
direct_throttle_control
```

Which are directly added to the data received from the control panel.
Probably, in the future, other data will be added, at least for working with GPS. Stay tuned for updates in our repository.

### 5.7. Camera gimbal

To operate our system in real conditions, it is required to minimize camera shaking if we don't want to lose the tag on the drone. For that reason, a 3D model of a gimbal for attaching the drone to the platform was developed to stabilize a conventional webcam.

Camera mount:

 ![Camera mount](https://github.com/XxOinvizioNxX/Liberty-Way/blob/main/git_images/gimbal_camera_mount.png "Camera mount")

Camera wire fixing (ferrite filter on the wire):

 ![Filter mount](https://github.com/XxOinvizioNxX/Liberty-Way/blob/main/git_images/gimbal_filter_mount.png "Filter mount")

Fixing of the "crabs" latches on the suspension substrate:

 ![Plane mount](https://github.com/XxOinvizioNxX/Liberty-Way/blob/main/git_images/gimbal_plane_mount.png "Plane mount")

An approximate view of the assembly of the entire suspension mechanism:

 ![Assembly](https://github.com/XxOinvizioNxX/Liberty-Way/blob/main/git_images/gimbal_assembly.png "Assembly")

-----------

## 6. Eitude AMLS Platform

The platform is an interconnected system for landing the drone. The platform was planned to be controlled via the Serial interface, using the G-Code commands: The current platform code can be found in the Eitude GitHub repository: https://github.com/XxOinvizioNxX/Eitude

### 6.1. Grabbing system

As you may know it doesn't matter how good is our stabilization, without grabbing system the drone will crash eventually. Hence we developed a 3D model of a grabbing system with 4 grips that have a hook at the end of each one. This will allow the system to slowly grab the drone while it lands and hold it steady after landing.

![Screenshot](https://github.com/XxOinvizioNxX/Liberty-Way/blob/main/git_images/grabbing_system_1.png "Screenshot")

![Screenshot](https://github.com/XxOinvizioNxX/Liberty-Way/blob/main/git_images/grabbing_system_2.png "Screenshot")

### 6.2. Weather protection system

As for the weather protection, we developed a 3D model to create a roof that will protect the drone from weather conditions while it is on the platform.
The AMLS weather protection system consists of scissor-like mechanisms covered with a canvas, which are located around the edges of the platform.
After a successful landing, the mechanisms on both sides of the platform close and protect the drone from external influences. The roof structure itself makes it light and strong, and the scissor-like mechanisms allow it to simply fold and unfold itself. Moreover, the assembly of such mechanisms will be simple and reliable.

![Screenshot](https://github.com/XxOinvizioNxX/Liberty-Way/blob/main/git_images/platform_side_transparent.png "Screenshot")

![Screenshot](https://github.com/XxOinvizioNxX/Liberty-Way/blob/main/git_images/platform_roof.png "Screenshot")

### 6.3. Platform speedometer

In order to land on a quickly moving platform, it is very useful to know its speed. For now, the platform does not have a GPS module, or any other way to measure absolute speed. Therefore, for a temporary solution to this problem, it was decided to calculate the speed using an accelerometer. For example, MPU6050. The IMU is mounted into the prototype platform through a soft backing and it's covered with a cap to protect it from the wind. The stabilization algorithm (Liberty-Way) sends a request to the platform `L1` to test the speed. A message `S0 L <speed in km / h>` is returned as a response.

![MPU6050](https://github.com/XxOinvizioNxX/Liberty-Way/blob/main/git_images/mpu6050_gyro.png "MPU6050")

Speedometer test (inside the gray circle, lower right parameter (SPD) - speed in km / h) (Clickable):
[![Watch the video](https://github.com/XxOinvizioNxX/Liberty-Way/blob/main/git_images/youtube_speedometer_test.jpg)](https://youtu.be/yvCo6tYjdM0)

To calculate the speed, the acceleration is taken for short time periods, then multiplied with time, which results with the instantaneous speed. Then this speed is constantly added to the previous value:

```cpp
void speed_handler(void) {
	gyro_signalen();

	// Filter accelerations
	acc_x_filtered = (float)acc_x_filtered * ACC_FILTER_KOEFF + (float)acc_x * (1.0 - ACC_FILTER_KOEFF);

	speed = acc_x_filtered;
	// Convert acceleration to G
	speed /= 4096.0;
	// Convert to m/s^2
	speed *= 9.81;	
	// Multiply by dt to get instant speed in m/ms
	speed *= (millis() - speed_loop_timer);

	// Reset timer
	speed_loop_timer = millis();

	// Convert to m/s
	speed /= 1000.0;						
	// Convert to km/h
	speed *= 3.6;						
	
	// Accumulate instantaneous speed
	speed_accumulator += speed;

	if (!in_move_flag) {
		// If the platform is not moving, reset the speed
		speed_accumulator = speed_accumulator * SPEED_ZEROING_FACTOR;
	}
}
```

Despite having various filters, due to the error, the speed may not "return" to 0, therefore, vibrations are also measured, and if they are less than the certain threshold, it is considered that the platform is at a standstill and the speed gradually resets to zero.

The complete code of the speedometer can be found in the Eitude repository on GitHub: https://github.com/XxOinvizioNxX/Eitude

### 6.4. Platform light sensor

As our platform must work in various environmental conditions, and optical stabilization is very demanding on the visibility of the ArUco marker, it is important to have an automatic system for measuring the camera exposure by the level of illumination around it, and turning on additional illumination if there is a lack of lighting. In the long term, it is planned to use specialized sensors, for example, the BH1750, as light sensors.

In the current version of the prototype, 6 LEDs are used as a light sensor and an ADC built into the microcontroller. The stabilization algorithm (Liberty-Way) sends a request `L0` to the platform to check the illumination level. A message `S0 L <luminance>` is returned as a response.

![Light sensors](https://github.com/XxOinvizioNxX/Liberty-Way/blob/main/git_images/light_sensors.png "Light sensors")

Test for determining the level of illumination using LEDs (clickable):
[![Watch the video](https://github.com/XxOinvizioNxX/Liberty-Way/blob/main/git_images/youtube_light_sensor.jpg)](https://www.youtube.com/watch?v=xQeiA945aRA&ab_channel=AMLSMosPolytech)

Exposure adjustment and adding additional illumination tests (clickable):
[![Watch the video](https://github.com/XxOinvizioNxX/Liberty-Way/blob/main/git_images/youtube_exposure_test.jpg)](https://youtu.be/iMORim6zxsg)

-----------

## 7. Conclusion

At the moment, there is a debugged prototype of optical stabilization, GPS holding, altitude stabilization via barometer, different platform prototypes and a great amount of 3D models eager to be constructed.
The project of the automatical landing of a drone onto a moving platform is not yet complete.

Follow the updates:

- In our repository GitHub: https://github.com/XxOinvizioNxX/Liberty-Way
- On our YouTube channel: https://www.youtube.com/channel/UCqN12Jzy-1eJLkcA32R0jdg

In the future, we plan to do much more new and interesting stuff!

![Follow the white rabbit](https://github.com/XxOinvizioNxX/Liberty-Way/blob/main/git_images/follow_the_white_rabbit.png "Follow the white rabbit")
