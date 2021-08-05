# Sonarus - I2C ultrasonic rangefinder

![Logo](/git_images/logo_book.png "Logo")

This project was originated as a part of project activity in Moscow Polytech University by students of group 181-311.
This project is currently part of the AMLS (Autonomous Multicopter Landing System) project. The AMLS project was created as a part of project activities subject of Moscow Polytech University by the 181-311 group

Sonarus I2C ultrasonic rangefinder © 2021 Pavel Neshumov (Fern Hertz)

![Sonarus](/git_images/sonarus.jpg "Sonarus")

----------

Sonarus is a system consisting of two HC-SR04 ultrasonic rangefinders connected to an Atmega328 microcontroller (Arduino).
The purpose of the system is to provide the ability to measure distance from two sensors via the I2C bus.

Currently, Sonarus is used on the Liberty-X drone in order to avoid collisions with obstacles, as well as to prevent accidental motors shutdown during landing.
The first sensor is in front of the drone and looks forward, the second is at the bottom of the drone and looks below.

----------
