package com.liberty_amls;

import jdk.jfr.Relational;

public class TelemetryContainer {
    public boolean telemetryLost;
    public int packetsNumber;
    public int errorStatus, flightMode;
    public double batteryVoltage, temperature;
    public int angleRoll, anglePitch, angleYaw;
    public int startStatus;
    public int altitude, takeoffThrottle;
    public boolean takeoffDetected, headingLock;
    public int fixType, satellitesNum;
    public int gpsLatInt, gpsLonInt;
    public double gpsLatDouble, gpsLonDouble;
    public int linkWaypointStep;
    public boolean linkNewWaypointAltitude, linkNewWaypointGPS;
    public Velocity velocity;
    public double loopTime;

    /**
     * This class contains all data from the telemetry
     */
    TelemetryContainer() {
        telemetryLost = true;
        errorStatus = 0;
        flightMode = 1;
        batteryVoltage = 0;
        temperature = 0;
        angleRoll = 0;
        anglePitch = 0;
        angleYaw = 0;
        startStatus = 0;
        altitude = 0;
        takeoffThrottle = 1500;
        takeoffDetected = false;
        headingLock = false;
        fixType = 0;
        satellitesNum = 0;
        gpsLatInt = 0;
        gpsLonInt = 0;
        gpsLatDouble = 0;
        gpsLonDouble = 0;
        linkWaypointStep = 0;
        linkNewWaypointAltitude = false;
        linkNewWaypointGPS = false;
        velocity = new Velocity();
        loopTime = System.currentTimeMillis() / 1000.0;
    }


    static class Velocity{
        public double velocityX, velocityY, overallVelocity;

        /**
         * This sub-class contains velocity of a drone
         */
        public Velocity() {
            this.velocityX = 0.0;
            this.velocityY = 0.0;
            this.overallVelocity = 0.0;
        }

        /**
         * This method sets new velocity for the drone
         * @param vX velocity got based on latitude change
         * @param vY velocity got based on longitude change
         */
        public void SetVelocity(double vX, double vY) {
            this.velocityX = vX;
            this.velocityY = vY;

            this.overallVelocity = Math.sqrt(vX*vX + vY*vY);
        }
    }
}
