/*
 * Copyright 2021 The Liberty-Way Landing System Open Source Project
 * This software is part of Autonomous Multirotor Landing System (AMLS) Project
 *
 * Licensed under the GNU Affero General Public License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.gnu.org/licenses/agpl-3.0.en.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.liberty_amls;

import com.google.gson.JsonObject;
import com.stormbots.MiniPID;
import org.apache.log4j.Logger;

public class PositionHandler {
    private final Logger logger = Logger.getLogger(this.getClass().getSimpleName());
    private final SettingsContainer settingsContainer;
    private final SerialHandler serialHandler;
    private final UDPHandler udpHandler;
    private final MiniPID miniPIDX, miniPIDY, miniPIDZ, miniPIDYaw;
    private final PositionContainer positionContainer;
    private final PlatformContainer platformContainer;
    private final TelemetryContainer telemetryContainer;
    private final BlackboxHandler blackboxHandler;
    private final GPSEstimationContainer gpsEstimationContainer;
    private final GPSEstimationHandler gpsEstimationHandler;
    private final byte[] directControlData;
    private int waypointStep = 0;
    private int lostCounter = 0;
    private boolean libertyWayEnabled = false, libertyWayEnabledLast = false;

    /**
     * This class takes the absolute coordinates of the marker as input,
     * passes them through the PID controllers,
     * generates Direct Control values and sends them to the drone via the Serial port or UDP
     * @param serialHandler SerialHandler class object to send data
     * @param udpHandler UDPHandler class object to send data
     */
    public PositionHandler(SerialHandler serialHandler,
                           UDPHandler udpHandler,
                           PositionContainer positionContainer,
                           PlatformContainer platformContainer,
                           TelemetryContainer telemetryContainer,
                           BlackboxHandler blackboxHandler,
                           SettingsContainer settingsContainer,
                           GPSEstimationContainer gpsEstimationContainer,
                           GPSEstimationHandler gpsEstimationHandler) {
        this.serialHandler = serialHandler;
        this.udpHandler = udpHandler;
        this.miniPIDX = new MiniPID(0, 0, 0, 0);
        this.miniPIDY = new MiniPID(0, 0, 0, 0);
        this.miniPIDZ = new MiniPID(0, 0, 0, 0);
        this.miniPIDYaw = new MiniPID(0, 0, 0, 0);
        this.directControlData = new byte[12];
        directControlData[10] = settingsContainer.dataSuffix1;
        directControlData[11] = settingsContainer.dataSuffix2;
        this.positionContainer = positionContainer;
        this.platformContainer = platformContainer;
        this.telemetryContainer = telemetryContainer;
        this.blackboxHandler = blackboxHandler;
        this.settingsContainer = settingsContainer;
        this.gpsEstimationContainer = gpsEstimationContainer;
        this.gpsEstimationHandler = gpsEstimationHandler;

        positionContainer.setSetpoints(settingsContainer.setpointX, settingsContainer.setpointY, 0,
                settingsContainer.setpointYaw);
        miniPIDX.setSetpoint(positionContainer.setpointX);
        miniPIDY.setSetpoint(positionContainer.setpointY);
        miniPIDZ.setSetpoint(positionContainer.setpointZ);
        miniPIDYaw.setSetpoint(positionContainer.setpointYaw);
    }

    /**
     * Calls if there is no marker in sight (newMarkerPosition = false)
     * @param newMarkerPosition must be false
     */
    public void proceedPosition(boolean newMarkerPosition) {
        proceedPosition(newMarkerPosition, 0, 0, 0, 0);
    }

    /**
     * Calls by OpenCV handler
     * @param newMarkerPosition set it to true if marker is in sight
     * @param x estimated marker X position (if newMarkerPosition)
     * @param y estimated marker Y position (if newMarkerPosition)
     * @param z estimated marker Z position (if newMarkerPosition)
     * @param yaw estimated marker Yaw angle (if newMarkerPosition)
     */
    public void proceedPosition(boolean newMarkerPosition, double x, double y, double z, double yaw) {
        // Reset DDC values (1500 = no correction)
        positionContainer.ddcX = 1500;
        positionContainer.ddcY = 1500;
        positionContainer.ddcZ = 1500;
        positionContainer.ddcYaw = 1500;

        // Reset PID controllers if mode is IDLE or not STAB, LAND, PREV
        if (positionContainer.status == 0 || positionContainer.status >= 4)
            resetPIDs();

        // Begin Liberty Way sequence if libertyWayEnabled, IDLE mode and platform and drone are available
        if (!settingsContainer.onlyOpticalStabilization && libertyWayEnabled && positionContainer.status == 0
                && !platformContainer.platformLost && !telemetryContainer.telemetryLost) {
            logger.warn("Caution! Starting LibertyWay sequence! Motor start possible!");
            positionContainer.status = 5;
        }

        if (newMarkerPosition) {
            // If the marker is detected
            // Filter position if status is STAB or LAND
            if (positionContainer.status == 1 || positionContainer.status == 2) {
                this.positionContainer.x = this.positionContainer.x * settingsContainer.inputFilter +
                        x * (1 - settingsContainer.inputFilter);
                this.positionContainer.y = this.positionContainer.y * settingsContainer.inputFilter +
                        y * (1 - settingsContainer.inputFilter);
                this.positionContainer.z = this.positionContainer.z * settingsContainer.inputFilter +
                        z * (1 - settingsContainer.inputFilter);
                this.positionContainer.yaw = this.positionContainer.yaw * settingsContainer.inputFilter +
                        yaw * (1 - settingsContainer.inputFilter);
            } else {
                // Don't filter in other modes (reset position memory) in other modes
                this.positionContainer.x = x;
                this.positionContainer.y = y;
                this.positionContainer.z = z;
                this.positionContainer.yaw = yaw;
            }

            if (libertyWayEnabled) {
                // If marker is in sight, landing is allowed, the altitude is lower than the threshold
                // and the status is LAND
                if (settingsContainer.landingAllowed && positionContainer.z <= settingsContainer.landingAlt
                        && positionContainer.status == 2) {
                    if (telemetryContainer.takeoffDetected) {
                        // Turn off the motors
                        logger.warn("Landed successfully! Turning off the motors.");
                        sendMotorsStop();
                    } else
                        positionContainer.status = 7;
                }

                // If marker is in sight, Liberty way is enabled, not finished yet and not in STAB or LAND modes
                if (positionContainer.status != 7 && positionContainer.status != 1 && positionContainer.status != 2) {
                    // Beginning of optical stabilization
                    // Set current mode to STAB
                    positionContainer.status = 1;
                    // Remember current position as setpoint
                    positionContainer.setpointX = positionContainer.x;
                    positionContainer.setpointY = positionContainer.y;
                    positionContainer.setpointZ = positionContainer.z;
                    positionContainer.entryZ = positionContainer.z;
                    logger.warn("Marker in sight! Setpoints fixed at X=" +
                            (int) positionContainer.setpointX +
                            " Y=" + (int) positionContainer.setpointY +
                            " Z=" + (int) positionContainer.setpointZ);
                    logger.warn("Start smooth alignment of setpoints");
                    resetPIDs();
                    miniPIDX.setSetpoint(positionContainer.setpointX);
                    miniPIDY.setSetpoint(positionContainer.setpointY);
                    miniPIDZ.setSetpoint(positionContainer.setpointZ);
                }
            }

            // Reset lost counter
            lostCounter = 0;
        }

        // Drone direct control (STAB, LAND, PREV modes)
        if (libertyWayEnabled && (positionContainer.status > 0 && positionContainer.status <= 3)) {
            // Setpoints alignment
            positionContainer.setpointX = positionContainer.setpointX * settingsContainer.setpointAlignmentFactor
                    + positionContainer.setpointAbsX * (1 - settingsContainer.setpointAlignmentFactor);
            positionContainer.setpointY = positionContainer.setpointY * settingsContainer.setpointAlignmentFactor
                    + positionContainer.setpointAbsY * (1 - settingsContainer.setpointAlignmentFactor);
            miniPIDX.setSetpoint(positionContainer.setpointX);
            miniPIDY.setSetpoint(positionContainer.setpointY);


            // If landing is enabled, current mode is STAB and landing conditions are met
            if (settingsContainer.landingAllowed && (positionContainer.status == 1 || positionContainer.status == 2)
                    && Math.abs(positionContainer.x - positionContainer.setpointAbsX) <
                    settingsContainer.allowedLandingRangeXY
                    && Math.abs(positionContainer.y - positionContainer.setpointAbsY) <
                    settingsContainer.allowedLandingRangeXY
                    && Math.abs(positionContainer.yaw - positionContainer.setpointYaw) <
                    settingsContainer.allowedLandingRangeYaw) {
                // Slowly lowering the altitude
                if (positionContainer.setpointZ > 1)
                    positionContainer.setpointZ -= settingsContainer.landingDecrement;
                miniPIDZ.setSetpoint(positionContainer.setpointZ);
                positionContainer.status = 2;
            }

            // Append corrections from PID controllers
            positionContainer.ddcX += miniPIDX.getOutput(positionContainer.x);
            positionContainer.ddcY += miniPIDY.getOutput(positionContainer.y);
            positionContainer.ddcZ += miniPIDZ.getOutput(positionContainer.z);
            positionContainer.ddcYaw += miniPIDYaw.getOutput(positionContainer.yaw);

            // Send direct correction to the drone
            sendDDC();
        }

        if (!newMarkerPosition) {
            // If no marker detected
            switch (positionContainer.status) {
                case 1:
                    // STAB (In optical stabilization)
                case 2:
                    // LAND (In optical landing)
                    logger.warn("The marker is lost! " +
                            "The previous position will be used for next " +
                            settingsContainer.allowedLostFrames + " frames!");
                    lostCounter++;
                    positionContainer.status = 3;
                    break;
                case 3:
                    // PREV (In optical stabilization but with previous position)
                    lostCounter++;
                    if (lostCounter > settingsContainer.allowedLostFrames) {
                        logger.error("The marker is completely lost! Stabilization will be terminated!");
                        // Reset DDC corrections
                        positionContainer.ddcX = 1500;
                        positionContainer.ddcY = 1500;
                        positionContainer.ddcZ = 1500;
                        positionContainer.ddcYaw = 1500;
                        // Send abort command
                        logger.warn("Sending abort command");
                        sendAbort();
                        positionContainer.status = 4;
                    }
                    break;
                case 4:
                    // LOST
                    if (!settingsContainer.onlyOpticalStabilization &&
                            !telemetryContainer.telemetryLost && !platformContainer.platformLost) {
                        // Enter WAYP loop if optical stabilization has been lost
                        logger.warn("Switching to WAYP mode");
                        sendIDLE();
                        positionContainer.status = 6;
                    } else {
                        // Send abort command
                        sendAbort();
                    }
                    break;
                case 5:
                    // TKOF
                    if (!platformContainer.platformLost && !telemetryContainer.telemetryLost) {
                        if (waypointStep == 0) {
                            // Step 0. Send altitude waypoint
                            if (telemetryContainer.linkNewWaypointAltitude)
                                waypointStep = 1;
                            sendPressureWaypoint(platformContainer.pressure);
                        } else if (waypointStep == 1) {
                            // Step 1. Send gps waypoint
                            if (telemetryContainer.linkNewWaypointGPS)
                                waypointStep = 2;
                            sendGPSWaypoint(platformContainer.gpsLatInt, platformContainer.gpsLonInt);
                            gpsEstimationContainer.arrayOfTrueGPS.add(new gpsEstimationContainer.TrueGPS(platformContainer.gpsLatInt,
                                    platformContainer.gpsLonInt));
                        } else {
                            if (telemetryContainer.takeoffDetected)
                                // Switch to WAYP mode if takeoff detected
                                positionContainer.status = 6;
                            else
                                // Send command to begin Liberty Way sequence (take off)
                                sendStartSequence();
                            waypointStep = 0;
                        }
                    } else
                        sendIDLE();
                    break;
                case 6:
                    // WAYP
                    if (!platformContainer.platformLost && !telemetryContainer.telemetryLost) {
                        if (waypointStep == 0) {
                            // Step 0. Send altitude waypoint
                            if (telemetryContainer.linkNewWaypointAltitude)
                                waypointStep = 1;
                            sendPressureWaypoint(platformContainer.pressure);
                        } else if (waypointStep == 1) {
                            // Step 1. Send gps waypoint
                            if (telemetryContainer.linkNewWaypointGPS)
                                waypointStep = 2;
                            if (distanceIsAcceptable()){
                                sendGPSWaypoint(platformContainer.gpsLatInt, platformContainer.gpsLonInt);
                                gpsEstimationContainer.arrayOfTrueGPS.add(new gpsEstimationContainer.TrueGPS(platformContainer.gpsLatInt,
                                        platformContainer.gpsLonInt));
                            }
                            else{
                                gpsEstimationContainer.arrayOfTrueGPS.add(new gpsEstimationContainer.TrueGPS(platformContainer.gpsLatInt,
                                        platformContainer.gpsLonInt));
                                gpsEstimationHandler.Calculate();
                                if (gpsEstimationContainer.arrayOfEstimatedGPS.size() != 0){
                                    var estimatedGPSArray = gpsEstimationContainer.arrayOfEstimatedGPS;
                                    var lastEstimatedGPS = estimatedGPSArray.get(estimatedGPSArray.size() - 1);

                                    sendGPSWaypoint(lastEstimatedGPS.latitude, lastEstimatedGPS.longitude);
                                }
                            }
                        } else if (waypointStep >= 2) {
                            // Step 2. Wait for both flags to complete
                            waypointStep++;
                            if (waypointStep > 4)
                                waypointStep = 0;
                            sendIDLE();
                        }
                    } else
                        sendIDLE();
                    break;
                default:
                    sendIDLE();
                    // Other modes
                    break;
            }
        }

        if (positionContainer.status != 7 && libertyWayEnabled) {
            // Sets newEntry flag in BlackboxHandler class
            blackboxHandler.setNewEntryFlag(true);
        } else
            blackboxHandler.setBlackboxEnabled(false);
    }

    /**
     * Enables or disables main Liberty-Way sequence
     */
    public void setLibertyWayEnabled(boolean libertyWayEnabled) {
        this.libertyWayEnabled = libertyWayEnabled;
        if (libertyWayEnabled != libertyWayEnabledLast) {
            if (libertyWayEnabled) {
                if (!telemetryContainer.telemetryLost && telemetryContainer.takeoffDetected)
                    // Send abort command if closed in flight
                    sendAbort();
                else
                    // Send IDLE command in other cases
                    sendIDLE();
            } else
                sendIDLE();
            // Reset current status to IDLE
            positionContainer.status = 0;
            // Reset waypoints step
            waypointStep = 0;
        }
        libertyWayEnabledLast = libertyWayEnabled;
    }

    /**
     * Sends IDLE command to the drone (disables all corrections and requests telemetry)
     * Link command = 0
     */
    private void sendIDLE() {
        // Reset "body" bytes
        for (int i = 0; i <= 7; i++)
            directControlData[i] = 0;

        // Link command
        directControlData[8] = (byte) 0;

        // Transmit data
        pushLinkData();
    }

    /**
     * Sends direct corrections (from positionContainer) to the drone (optical stabilization & landing)
     * Link command = 1
     */
    private void sendDDC() {
        // Convert X and Y to Roll and Pitch
        double yawSin = Math.sin(Math.toRadians(-positionContainer.yaw));
        double yawCos = Math.cos(Math.toRadians(-positionContainer.yaw));
        positionContainer.ddcPitch = (int)((positionContainer.ddcX - 1500) * yawCos
                - (positionContainer.ddcY - 1500) * yawSin + 1500);
        positionContainer.ddcRoll = (int)((positionContainer.ddcX - 1500) * yawSin
                + (positionContainer.ddcY - 1500) * yawCos + 1500);

        // Form the DDC data package
        // Roll
        directControlData[0] = (byte) ((positionContainer.ddcRoll >> 8) & 0xFF);
        directControlData[1] = (byte) (positionContainer.ddcRoll & 0xFF);
        // Pitch
        directControlData[2] = (byte) ((positionContainer.ddcPitch >> 8) & 0xFF);
        directControlData[3] = (byte) (positionContainer.ddcPitch & 0xFF);
        // Yaw
        directControlData[4] = (byte) ((positionContainer.ddcYaw >> 8) & 0xFF);
        directControlData[5] = (byte) (positionContainer.ddcYaw & 0xFF);
        // Throttle
        directControlData[6] = (byte) ((positionContainer.ddcZ >> 8) & 0xFF);
        directControlData[7] = (byte) (positionContainer.ddcZ & 0xFF);
        // Link command (1 - direct control)
        directControlData[8] = (byte) 1;

        // Transmit direct control data
        pushLinkData();
    }

    /**
     * Sends pressure waypoint to the drone
     * Link command = 2
     * @param pressure atm. pressure in Pascals
     */
    private void sendPressureWaypoint(int pressure) {
        // Pressure waypoint
        directControlData[0] = (byte) ((pressure >> 24) & 0xFF);
        directControlData[1] = (byte) ((pressure >> 16) & 0xFF);
        directControlData[2] = (byte) ((pressure >> 8) & 0xFF);
        directControlData[3] = (byte) (pressure & 0xFF);

        // Empty other part of the packet
        directControlData[4] = 0;
        directControlData[5] = 0;
        directControlData[6] = 0;
        directControlData[7] = 0;

        // Link command
        directControlData[8] = (byte) 2;

        // Transmit pressure waypoint
        pushLinkData();
    }

    /**
     * Sends gps waypoint
     * Link command = 3
     * @param latInt signed integer latitude (from -90000000 to 90000000)
     * @param lonInt signed integer longitude (from -180000000 to 180000000)
     */
    private void sendGPSWaypoint(int latInt, int lonInt) {
        // Lat
        directControlData[0] = (byte) ((latInt >> 24) & 0xFF);
        directControlData[1] = (byte) ((latInt >> 16) & 0xFF);
        directControlData[2] = (byte) ((latInt >> 8) & 0xFF);
        directControlData[3] = (byte) (latInt & 0xFF);

        // Lon
        directControlData[4] = (byte) ((lonInt >> 24) & 0xFF);
        directControlData[5] = (byte) ((lonInt >> 16) & 0xFF);
        directControlData[6] = (byte) ((lonInt >> 8) & 0xFF);
        directControlData[7] = (byte) (lonInt & 0xFF);

        // Link command
        directControlData[8] = (byte) 3;

        // Transmit GPS coordinates
        pushLinkData();
    }

    /**
     * Sends a command to turn off the motors.
     * Link command = 4
     */
    private void sendMotorsStop() {
        // Reset "body" bytes
        for (int i = 0; i <= 7; i++)
            directControlData[i] = 0;

        // Link command
        directControlData[8] = (byte) 4;

        // Transmit data
        pushLinkData();
    }

    /**
     * Sends a command to turn off the motors.
     * Link command = 5
     */
    private void sendStartSequence() {
        logger.warn("Sending start command");
        // Reset "body" bytes
        for (int i = 0; i <= 7; i++)
            directControlData[i] = 0;

        // Link command
        directControlData[8] = (byte) 5;

        // Transmit data
        pushLinkData();
    }

    /**
     * Sends abort command to the drone
     * (Clears flags, resets direct corrections, waypoint flags and sharply jumps up to prevent a collision)
     * Link command = 6
     */
    private void sendAbort() {
        // Reset "body" bytes
        for (int i = 0; i <= 7; i++)
            directControlData[i] = 0;

        // Link command
        directControlData[8] = (byte) 6;

        // Transmit data
        pushLinkData();
    }

    /**
     * Pushes bytes buffer to Liberty-Link port via Serial or UDP
     */
    private void pushLinkData() {
        // Check byte
        byte checkByte = 0;
        for (int i = 0; i <= 8; i++)
            checkByte = (byte) (checkByte ^ directControlData[i]);
        directControlData[9] = checkByte;

        // Transmit data
        serialHandler.setLinkData(directControlData);
        udpHandler.setUdpData(directControlData);
        serialHandler.pushLinkData();
        udpHandler.pushData();
    }

    /**
     * Resets PID controllers
     */
    private void resetPIDs() {
        miniPIDX.reset();
        miniPIDY.reset();
        miniPIDZ.reset();
        miniPIDYaw.reset();
    }

    /**
     * Sets coefficients from json
     */
    public void loadPIDFromFile() {
        // Load file
        JsonObject pids = FileWorkers.loadJsonObject(settingsContainer.pidFile);
        // X
        setupPID(pids.get("pid_x").getAsJsonObject(), miniPIDX);
        // Y
        setupPID(pids.get("pid_y").getAsJsonObject(), miniPIDY);
        // Z
        setupPID(pids.get("pid_z").getAsJsonObject(), miniPIDZ);
        // Yaw
        setupPID(pids.get("pid_yaw").getAsJsonObject(), miniPIDYaw);
    }

    /**
     * Sets parameters of the current PID
     * @param pid JsonObject PID (from file)
     * @param miniPID (MiniPID class)
     */
    private void setupPID(JsonObject pid, MiniPID miniPID) {
        miniPID.setDirection(pid.get("reversed").getAsBoolean());
        miniPID.setPID(pid.get("P").getAsDouble(), pid.get("I").getAsDouble(),
                pid.get("D").getAsDouble(), pid.get("F").getAsDouble());
        miniPID.setOutputRampRate(pid.get("ramp").getAsDouble());
        miniPID.setOutputLimits(pid.get("limit").getAsDouble());
    }

    /**
     * Calculates whether the current distance
     * between the platform and the drone is
     * acceptable enough for the drone to receive
     * non-processed GPS-coordinates
     * @return Conclusion about whether the drone is close enough
     */
    private boolean distanceIsAcceptable(){

        double lat1 = platformContainer.gpsLatDouble;
        double lon1 = platformContainer.gpsLonDouble;
        double lat2 = telemetryContainer.gpsLatDouble;
        double lon2 = telemetryContainer.gpsLonDouble;

        double dLat = (lat2-lat1) * Math.PI / 180;
        double dLon = (lon2-lon1) * Math.PI / 180;

        lat1 *= Math.PI / 180;
        lat2 *= Math.PI / 180;

        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.sin(dLon/2) * Math.sin(dLon/2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        double distance = (positionContainer.earthRadiusM * c);

        return distance < positionContainer.acceptableDistance;
    }
}
