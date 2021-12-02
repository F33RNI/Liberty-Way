# Liberty Drones - EN

<div style="width:100%;text-align:center;">
    <p align="center">
        <img src="https://github.com/XxOinvizioNxX/Liberty-Way/raw/main/git_images/logo_book.png" width="auto" height="auto">
    </p>
</div>

### The goal

Our team aims to create a system for autonomous control, tracking and landing of drones as a part of a bigger delivery system.
Our system is capable of guiding the drone towards GPS waypoints, stabilizing it over the platform using GPS and optical stabilizations, and performing capture and landing.

### Liberty Drones Article

This Article describes the Libery Drones project which consist of Liberty-X, Eitude, Sonarus, GPS Mixer and GPS to Serial modules. And the instruction of how it can be created on your own.

### Our main GitHub repository

[Liberty-Way](https://github.com/XxOinvizioNxX/Liberty-Way)

### Developers

- [Pavel Neshumov](mailto:xxoinvizionxx@gmail.com)
- [Andrey Kabalin](mailto:astik452@gmail.com)
- [Vladislav Yasnetsky](mailto:vlad.yasn@gmail.com)

-----------

## Table of contents

### TO-DO LATER ❗❗❗❗❗

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

## What is Liberty Drones?

The main project consist of several subprojects that are:

#### Liberty-X

Drone flight controller firmware for STM32 microcontroller. This flight controller is suitable for camera drones, scientific drones or delivery drones.

- https://github.com/XxOinvizioNxX/Liberty-X

#### Eitude

Liberty Drones Platform controller. This is a fully functional and reliable part of the Liberty Drones system from the platform perspective. Controller will be operating with such data as its own GPS location, level of illumination around it, its speed and the data that would be sent from the drone which is described in Data packet structure paragraph in Liberty-Way Project. So far, this system is able to measure the level of illumination of the surroundings, the speed of the platform, and also turn on/off the backlight.

- https://github.com/XxOinvizioNxX/Liberty-Way/tree/main/Eitude

#### Sonarus

Sonarus I2C ultrasonic rangefinder. Sonarus is a system consisting of two HC-SR04 ultrasonic rangefinders connected to an Atmega328 microcontroller (Arduino). The purpose of the system is to provide the ability to measure distance from two sensors via the I2C bus. Currently, Sonarus is used on the Liberty-X drone in order to avoid collisions with obstacles, as well as to prevent accidental motors shutdown during landing. The first sensor is at front of the drone and looks forward, the second is at the bottom of the drone and looks below.

- https://github.com/XxOinvizioNxX/Liberty-Way/tree/main/Sonarus

#### GPS Mixer

Way to improve GPS navigation using multiple receivers. GPS Mixer uses several low-cost GPS receivers (Ublox NEO-M8n) to improve the accuracy and reliability of GPS coordinates. Up to 3 receivers are currently in use.

GPS Mixer takes data from available receivers and calculates the arithmetic mean of the coordinates. Also, this solution will allow the drone to continue flying in the case of losing 1 or 2 receivers.

- https://github.com/XxOinvizioNxX/Liberty-Way/tree/main/GPS-mixer

#### GPS to Serial

Android app to send phone GPS coordinates via USB serial port.

- https://github.com/XxOinvizioNxX/GPS-to-Serial

-----------

#### How it works

Two main parts of the whole system are:

- The drone

<div style="width:100%;text-align:center;">
    <p align="center">
        <img src="https://github.com/XxOinvizioNxX/Liberty-Way/raw/main/git_images/liberty-x_side_cutout_2_small.png" width="400" height="auto">
    </p>
</div>

- And the platform

<div style="width:100%;text-align:center;">
    <p align="center">
        <img src="https://github.com/XxOinvizioNxX/Liberty-Way/raw/main/git_images/platform_side_transparent.png" width="600" height="auto">
    </p>
</div>

Basic description of the whole process:

- Firstly, the drone with a delivery package is far from the platform and it has no visual contact with it. The drone receives GPS coordinates of a platform by using cellular communication or any other radio channel (the drone has Liberty-Link implemented on it). This module is able to adjust its position, whatever the firmware of the flight controller. The module is installed inside the line between the receiver and the flight controller.
- The drone is moving to received coordinates. The coordinates might be renewed in the process (but not frequently, thus preventing the channel from overloading).
- When the drone is close to the platform but there is still no visual contact, the program runs GPS stabilization. Here the data is being transmitted over the closest radio communication channel of high frequency, so the drone can catch up with the platform.
- Meanwhile, the drone descends (barometers are installed on both, the drone and the platform). Descending proceeds until altitude reaches 1.5-2 meters above the platform.
- While descending and when visual contact with the platform camera is established, the program enables visual (precision) stabilization. And as soon as the drone's tag is within camera's field of view, the algorithm will capture the drone.
- When optical stabilization is enabled, GPS is working as a back up plan (in the case that something goes wrong, GPS stabilization launches again).
- In order to use optical stabilization the drone is equipped with ArUco tag which can be captured by a camera. By using the closest radio communication channel, the system transmits adjustment data to the drone.
- Along with optical stabilization, the program launches landing algorithm. The algorithm artificially and smoothly reduces the setpoint of height (Z) until it reaches a certain threshold.
- When the drone is near the desirable height, the program enables grabbing system implemented on the platform. Those grips are used to catch and hold the drone in the process of landing and after the drone was caught.
- When the landing has finished, the platform starts maintenance work and in order to protect the drone from external impact, the program enables weather protection and closes the roof above the landing area.
- Landing accomplished!

#### GPS hold and Flight to waypoints functions

As stated earlier, the drone is equipped with "universal" module Liberty-Link, which is receiving commands from the platform and adjusting the drone's position by interfering into the remote control signal (More in the following paragraphs).

Liberty-Link is provided with GPS Mixed which has to have up to 3 UBlox modules in order to minimize the error, so it would have the ability to maintain the drone's GPS position and follow GPS points.

<div style="width:100%;text-align:center;">
    <p align="center">
        <img src="https://github.com/XxOinvizioNxX/Liberty-Way/raw/main/git_images/liberty-x_front_cutout_2_small_gps.png" width="400" height="auto">
    </p>
</div>

GPS Mixer and Liberty-Link connect via UART that is configured to send data 5 times per second. Then Liberty-Link firmware will read data from the modules and calculate the coordinates of the current position.

But, for now, GPS Mixer is presented as a smartphone that generates GPS coordinates (for more info refer to [GPS to Serial](#15-gps-to-serial)).

#### Compass and barometer

Before optical stabilization launches (during GPS stabilization process), to calculate the GPS correction vector, you need to know the exact angle that the drone is rotated by. For this, a compass built into the GPS module is used. Because during the flight, the roll and pitch angles change and a user needs to correct the values from the compass.

It is clear that the angle from the compass can also be used to maintain the yaw angle of the drone. With point-to-point flights, this may be realized. But at the moment, there is no urgent need for this, because after the start of optical stabilization, the algorithm is able to correct the drone regardless of its yaw angle because the program shifts it automatically.

Before optical stabilization launches (during GPS stabilization process), our Liberty-Link module will be able to maintain altitude using a barometer.

The platform, as well as the Liberty-Link, will have MS5611 barometers.

According to the documentation, the height resolution is 10 cm. The algorithm will take the pressure values and by passing them through the PID-controller the drone's altitude will be stabilized via changing the Throttle (3rd channel).

Altitude hold test (clickable):

<div style="width:100%;text-align:center;">
    <p align="center">
    <a href="https://youtu.be/xmvcGeZzEfc">
        <img src="https://github.com/XxOinvizioNxX/Liberty-Way/raw/main/git_images/youtube_pressure_holding.jpg" width="600" height="auto">
        </a>
    </p>
</div>

During the flight along the waypoint, the setpoint of the pressure will decrease in order to increase the altitude (it is safer to fly in a straight line at a higher altitude, so the drone would not crash into anything). And during GPS stabilization (when the drone is already close to the platform), the drone will be set with a setpoint of pressure that correlates with ~ 1.5 - 2 m above the platform.

#### Optical stabilization

A cross-platform web sarvar application which proved to be very convenient for configuration and debugging. Which has a blackbox feature for recording logs, as well as communication with the platform and many other necessary algorithms.

Example of the operating stabilization (clickable):

<div style="width:100%;text-align:center;">
    <p align="center">
    <a href="https://www.youtube.com/watch?v=8vB-8QIBoJU&ab_channel=AMLSMosPolytech">
        <img src="https://github.com/XxOinvizioNxX/Liberty-Way/raw/main/git_images/youtube_holding_in_motion.jpg" width="600" height="auto">
        </a>
    </p>
</div>

All basic settings are conveniently placed in separate JSON files (settings, PID), which allow a user to quickly change required parameters without rebuilding the application. In fact, to run the application, you just need to download the latest release, unpack the archive and run it through the launcher corresponding to the preferable OS.

Liberty-Way connects to Liberty-Link module installed on the drone and adjusts its position by directly controlling four main channels of the remote control. In one cycle (each frame from the camera), 12 bytes of correction data are sent to the module.

Both settings and bytes are described more thoroughly in [our main repository's description](https://github.com/XxOinvizioNxX/Liberty-Way).

-----------

#### Eitude Platform

The platform is a system for landing the drone. The platform was planned to be controlled via the Serial interface, using the G-Code commands.

Considering that our platform must work in various environmental conditions, and good visibility of the ArUco marker is crucial for optical stabilization, it is important to have an automatic system for measuring the camera exposure by the level of illumination around it, and turning on additional illumination if there is a lack of lighting. In the long term, it is planned to use specialized sensors, for example, the BH1750, as light sensors.

<div style="width:100%;text-align:center;">
    <p align="center">
        <img src="https://github.com/XxOinvizioNxX/Liberty-Way/raw/main/git_images/light_sensors.png" width="450" height="auto">
    </p>
</div>

For more details please refer to [Eitude](https://github.com/XxOinvizioNxX/Liberty-Way/tree/main/Eitude).

-----------

## How to build it
  
#### Build the frame

First off all we have to assemble the drone's frame from 3d printed and easily accessible materials bought from any construction store. Parts and .stl docs for 3d printing are listed down below:

❗ List of parts

Now let's assemble the frame with step by step instruction:

❗ Instruction.pdf

#### Assembly Liberty-X controller

To build the controller, you have to buy and gather all the electrical parts from the list down below:

❗ List of parts

And then connect them as such:

❗ Principal electrical schema

#### Add Sonarus system

For the Sonarus system you will need these components:

❗ List of parts

Which are connected likewise:

❗ Picture of connection, I guess

#### Add GPS Mixer system

For the implementation of GPS Mixer you need these components to be installed on the drone's side:

❗ List of parts for the drone

And these to be installed onto the platform:

❗ List of parts for the platform

The drone's part is laid out like this:

❗ Drone connection schematic

In order to attach GPS Mixer to Eitude you have to make Eitude itself first!

#### Build Eitude platform

So for the platform you need to gather lightweight materials with similar parameters like those we used:

❗ List of platform contents

Then you will need to prepare these electronic parts:

❗ List of platform electronics

And lay them out as such:

❗ Platform electronics schematic

After Eitude will have been set as 'ready to go' then you yourself are ready to attach GPS Mixer to it just like this:

❗ GPS Mixer to patform connection schematic

## Conclusion

At the moment, there is a debugged prototype of optical stabilization, GPS holding, altitude stabilization via barometer, different platform prototypes and a great amount of 3D models eager to be constructed.
The project of the automatical landing of a drone onto a moving platform is not yet complete.

Follow the updates:

- In our repository [GitHub](https://github.com/XxOinvizioNxX/Liberty-Way).
- On our [YouTube channel](https://www.youtube.com/channel/UCqN12Jzy-1eJLkcA32R0jdg).
- In our [Twitter account](https://twitter.com/liberty_drones).

In the future, we plan to do much more new and interesting stuff!

<div style="width:100%;text-align:center;">
    <p align="center">
        <img src="https://github.com/XxOinvizioNxX/Liberty-Way/raw/main/git_images/follow_the_white_rabbit.png" width="auto" height="auto">
    </p>
</div>
