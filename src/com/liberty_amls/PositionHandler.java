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
    private final SerialHandler serialHandler;
    private final UDPHandler udpHandler;
    private final MiniPID miniPIDX, miniPIDY, miniPIDZ, miniPIDYaw;
    private double ddcX, ddcY, ddcZ, ddcYaw;
    private final byte[] directControlData;
    private int lostFrames;
    private double x = 0, y = 0, z = 0, yaw = 0;
    private double setpointX, setpointY, setpointZ, setpointYaw;
    private int status = 0, statusLast = 0;
    private int lostCounter = 0;
    private int pushOSDAfterFrames, osdFramesCounter = 0;
    private double lostKoeff;
    private double landingDecrement;
    private double allowedLandingRangeXY, allowedLandingRangeYaw;
    public final OSDHandler osdHandler;
    public double filterKoeff;
    public boolean stabEnabled = false;
    public boolean landingEnabled = false;
    public double landingAltitude = 0;

    /**
     * This class takes the absolute coordinates of the marker as input,
     * passes them through the PID controllers,
     * generates Direct Control values and sends them to the drone via the Serial port or UDP
     * @param serialHandler SerialHandler class object to send data
     * @param udpHandler UDPHandler class object to send data
     * @param osdHandler OSDHandler object to draw debug frame
     */
    public PositionHandler(SerialHandler serialHandler, UDPHandler udpHandler, OSDHandler osdHandler) {
        this.serialHandler = serialHandler;
        this.udpHandler = udpHandler;
        this.osdHandler = osdHandler;
        this.miniPIDX = new MiniPID(0, 0, 0, 0);
        this.miniPIDY = new MiniPID(0, 0, 0, 0);
        this.miniPIDZ = new MiniPID(0, 0, 0, 0);
        this.miniPIDYaw = new MiniPID(0, 0, 0, 0);
        this.directControlData = new byte[12];
    }

    /**
     * Proceed current position (new or predicted)
     */
    private void proceedPosition() {
        // reset DDC values (1500 = no correction)
        ddcX = 1500;
        ddcY = 1500;
        ddcZ = 1500;
        ddcYaw = 1500;

        // If the altitude is lower than the threshold
        if (landingEnabled && z <= landingAltitude
                && (statusLast == 2 || statusLast == 5)) {
            // Consider that the landing is complete
            // Repeat serviceInfo 3 (turn off the motors) continuously
            status = 5;
            if (statusLast == 2) {
                logger.warn("Landed successfully! Turning off the motors.");
            }
            TransmitPosition(3);
        }

        // If status is IDLE, LOST or DONE
        if (status == 0 || status >= 4) {
            // Reset PIDs and quit
            resetPIDs();
            return;
        }

        // If last status is IDLE or LOST
        if (statusLast == 0 || statusLast == 4) {
            // Beginning of stabilization
            setpointZ = z;
            logger.warn("Marker in sight! Stabilization started at altitude " + (int)setpointZ + " cm");
            miniPIDZ.setSetpoint(setpointZ);
            resetPIDs();
        }

        // If landing is enabled, position is not predicted and landing conditions are met
        if (landingEnabled && status != 3 && Math.abs(x - setpointX) < allowedLandingRangeXY
                && Math.abs(y - setpointY) < allowedLandingRangeXY
                && Math.abs(yaw - setpointYaw) < allowedLandingRangeYaw) {
            // Slowly lowering the altitude
            if (setpointZ > 1)
                setpointZ -= landingDecrement;
            miniPIDZ.setSetpoint(setpointZ);
            status = 2;
        }

        // Append corrections from PID controllers
        ddcX += miniPIDX.getOutput(x);
        ddcY += miniPIDY.getOutput(y);
        ddcZ += miniPIDZ.getOutput(z);
        ddcYaw += miniPIDYaw.getOutput(yaw);

        // Cast DDC value to the integer and transfer them to the OSD class
        osdHandler.ddcX = (int) ddcX;
        osdHandler.ddcY = (int) ddcY;
        osdHandler.ddcZ = (int) ddcZ;
        osdHandler.ddcYaw = (int) ddcYaw;

        TransmitPosition(1);
    }

    /**
     * Send corrections to the drone via Serial or UDP
     * @param serviceInfo Service Info for drone
     * 0 - Nothing to do, 1 - Stabilization, 2 - Landing (command not implemented), 3 - Disable motors
     */
    public void TransmitPosition(int serviceInfo) {
        // Convert X and Y to Roll and Pitch
        double yawSin = Math.sin(Math.toRadians(yaw));
        double yawCos = Math.cos(Math.toRadians(yaw));
        double ddcPitch = (ddcX - 1500) * yawCos - (ddcY - 1500) * yawSin + 1500;
        double ddcRoll = (ddcX - 1500) * yawSin + (ddcY - 1500) * yawCos + 1500;

        // Form the DDC data package
        // Roll
        directControlData[0] = (byte) (((int)ddcRoll >> 8) & 0xFF);
        directControlData[1] = (byte) ((int)ddcRoll & 0xFF);
        // Pitch
        directControlData[2] = (byte) (((int)ddcPitch >> 8) & 0xFF);
        directControlData[3] = (byte) ((int)ddcPitch & 0xFF);
        // Yaw
        directControlData[4] = (byte) (((int)ddcYaw >> 8) & 0xFF);
        directControlData[5] = (byte) ((int)ddcYaw & 0xFF);
        // Throttle
        directControlData[6] = (byte) (((int)ddcZ >> 8) & 0xFF);
        directControlData[7] = (byte) ((int)ddcZ & 0xFF);
        // Service info
        directControlData[8] = (byte) (serviceInfo & 0xFF);

        // Check byte
        byte checkByte = 0;
        for (int i = 0; i <= 8; i++)
            checkByte = (byte) (checkByte ^ directControlData[i]);
        directControlData[9] = checkByte;

        // Transmit data
        serialHandler.rfData = directControlData;
        udpHandler.udpData = directControlData;
        serialHandler.pushData();
        udpHandler.pushData();
    }

    /**
     * Calls when new position presented
     * @param x estimated marker X position
     * @param y estimated marker Y position
     * @param z estimated marker Z position
     * @param yaw estimated marker Yaw angle
     */
    public void newPosition(double x, double y, double z, double yaw) {
        // Filter position
        this.x = x * filterKoeff + this.x * (1 - filterKoeff);
        this.y = y * filterKoeff + this.y * (1 - filterKoeff);
        this.z = z * filterKoeff + this.z * (1 - filterKoeff);
        this.yaw = yaw * filterKoeff + this.yaw * (1 - filterKoeff);
        osdHandler.x = this.x;
        osdHandler.y = this.y;
        osdHandler.z = this.z;
        osdHandler.yaw = this.yaw;
        // Update status
        if (!landingEnabled || status != 5) {
            // If not landed
            if (stabEnabled)
                status = 1;
            else
                status = 0;
        }
        // Reset lost counter
        lostCounter = 0;
        // Update new position
        proceedPosition();
        statusLast = status;
        // OSD
        proceedOSD();
    }

    /**
     * Calls when no position estimated
     */
    public void noMarker() {
        switch (status) {
            case 1:
            case 2:
                // STAB
                logger.warn("The marker is lost! " +
                        "The predicted position will be used for next " + lostFrames + " frames!");
                lostCounter++;
                status = 3;
                break;
            case 3:
                // PRED
                x = x * lostKoeff + setpointX * (1 - lostKoeff);
                y = y * lostKoeff + setpointY * (1 - lostKoeff);
                z = z * lostKoeff + setpointZ * (1 - lostKoeff);
                yaw = yaw * lostKoeff + setpointYaw * (1 - lostKoeff);
                lostCounter++;
                if (lostCounter >= lostFrames) {
                    logger.error("The marker is completely lost! Stabilization will be terminated!");
                    ddcX = 1500;
                    ddcY = 1500;
                    ddcZ = 1500;
                    ddcYaw = 1500;
                    logger.error("Sending 0-corrections.");
                    TransmitPosition(1);
                    status = 4;
                }
            default:
                break;
        }
        proceedPosition();
        statusLast = status;
        // OSD
        proceedOSD();
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
     * Sets newPosition flag every pushOSDAfterFrames frame
     */
    private void proceedOSD() {
        osdHandler.status = status;
        osdFramesCounter++;
        if (osdFramesCounter == pushOSDAfterFrames) {
            osdHandler.newPositionFlag = true;
            osdFramesCounter = 0;
        }
    }

    /**
     * Load variables from json settings
     * @param settings JsonObject loaded from settings file
     */
    public void loadSettings(JsonObject settings) {
        filterKoeff = settings.get("input_filter").getAsDouble();
        lostKoeff = settings.get("lost_filter").getAsDouble();
        landingDecrement = settings.get("landing_decrement").getAsDouble();
        allowedLandingRangeXY = settings.get("allowed_landing_range_xy").getAsDouble();
        allowedLandingRangeYaw = settings.get("allowed_landing_range_yaw").getAsDouble();
        lostFrames = settings.get("allowed_lost_frames").getAsInt();
        pushOSDAfterFrames = settings.get("push_osd_after_frames").getAsInt();
        directControlData[10] = settings.get("data_suffix_1").getAsString().getBytes()[0];
        directControlData[11] = settings.get("data_suffix_2").getAsString().getBytes()[0];
        setpointX = settings.get("setpoint_x").getAsDouble();
        setpointY = settings.get("setpoint_y").getAsDouble();
        setpointYaw = settings.get("setpoint_yaw").getAsDouble();
        miniPIDX.setSetpoint(setpointX);
        miniPIDY.setSetpoint(setpointY);
        miniPIDZ.setSetpoint(setpointZ);
        miniPIDYaw.setSetpoint(setpointYaw);
    }

    /**
     * Sets coefficients from json
     * @param pids JsonObject pids file (default: pid.json)
     */
    public void setPIDFromJson(JsonObject pids) {
        // X
        JsonObject pid = pids.get("pid_x").getAsJsonObject();
        miniPIDX.setDirection(pid.get("reversed").getAsBoolean());
        miniPIDX.setPID(pid.get("P").getAsDouble(), pid.get("I").getAsDouble(),
                pid.get("D").getAsDouble(), pid.get("F").getAsDouble());
        miniPIDX.setOutputLimits(pid.get("limit").getAsDouble());
        // Y
        pid = pids.get("pid_y").getAsJsonObject();
        miniPIDY.setDirection(pid.get("reversed").getAsBoolean());
        miniPIDY.setPID(pid.get("P").getAsDouble(), pid.get("I").getAsDouble(),
                pid.get("D").getAsDouble(), pid.get("F").getAsDouble());
        miniPIDY.setOutputLimits(pid.get("limit").getAsDouble());
        // Z
        pid = pids.get("pid_z").getAsJsonObject();
        miniPIDZ.setDirection(pid.get("reversed").getAsBoolean());
        miniPIDZ.setPID(pid.get("P").getAsDouble(), pid.get("I").getAsDouble(),
                pid.get("D").getAsDouble(), pid.get("F").getAsDouble());
        miniPIDZ.setOutputLimits(pid.get("limit").getAsDouble());
        // Yaw
        pid = pids.get("pid_yaw").getAsJsonObject();
        miniPIDYaw.setDirection(pid.get("reversed").getAsBoolean());
        miniPIDYaw.setPID(pid.get("P").getAsDouble(), pid.get("I").getAsDouble(),
                pid.get("D").getAsDouble(), pid.get("F").getAsDouble());
        miniPIDYaw.setOutputLimits(pid.get("limit").getAsDouble());
    }
}
