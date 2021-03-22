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
    private final PlatformHandler platformHandler;
    private final MiniPID miniPIDX, miniPIDY, miniPIDZ, miniPIDYaw;
    private final PositionContainer positionContainer;
    private final PlatformContainer platformContainer;
    private final BlackboxHandler blackboxHandler;
    private final byte[] directControlData;
    private int lostFrames;
    private int statusLast = 0;
    private int lostCounter = 0;
    private int pushOSDAfterFrames, osdFramesCounter = 0;
    private double landingDecrement;
    private double allowedLandingRangeXY, allowedLandingRangeYaw;
    public final OSDHandler osdHandler;
    public double inputFilter, setpointAlignmentFactor;
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
    public PositionHandler(SerialHandler serialHandler,
                           UDPHandler udpHandler,
                           PlatformHandler platformHandler,
                           OSDHandler osdHandler,
                           PositionContainer positionContainer,
                           PlatformContainer platformContainer,
                           BlackboxHandler blackboxHandler) {
        this.serialHandler = serialHandler;
        this.udpHandler = udpHandler;
        this.platformHandler = platformHandler;
        this.osdHandler = osdHandler;
        this.miniPIDX = new MiniPID(0, 0, 0, 0);
        this.miniPIDY = new MiniPID(0, 0, 0, 0);
        this.miniPIDZ = new MiniPID(0, 0, 0, 0);
        this.miniPIDYaw = new MiniPID(0, 0, 0, 0);
        this.directControlData = new byte[12];
        this.positionContainer = positionContainer;
        this.platformContainer = platformContainer;
        this.blackboxHandler = blackboxHandler;
    }

    /**
     * Proceed current position (new or predicted)
     */
    private void proceedPosition() {
        // reset DDC values (1500 = no correction)
        positionContainer.ddcX = 1500;
        positionContainer.ddcY = 1500;
        positionContainer.ddcZ = 1500;
        positionContainer.ddcYaw = 1500;

        // If the altitude is lower than the threshold
        if (landingEnabled && positionContainer.z <= landingAltitude
                && (statusLast == 2 || statusLast == 5)) {
            // Consider that the landing is complete
            // Repeat serviceInfo 3 (turn off the motors) continuously
            positionContainer.status = 5;
            if (statusLast == 2) {
                logger.warn("Landed successfully! Turning off the motors.");
            }
            TransmitPosition(3);
        }

        // If status is IDLE, LOST or DONE
        if (positionContainer.status == 0 || positionContainer.status >= 4) {
            // Reset PIDs and quit
            resetPIDs();
            return;
        }

        // If last status is IDLE or LOST
        if (statusLast == 0 || statusLast == 4) {
            // Beginning of stabilization
            positionContainer.setpointX = positionContainer.x;
            positionContainer.setpointY = positionContainer.y;
            positionContainer.setpointZ = positionContainer.z;
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

        // Setpoints alignment
        positionContainer.setpointX = positionContainer.setpointX * setpointAlignmentFactor
                + positionContainer.setpointAbsX * (1 - setpointAlignmentFactor);
        positionContainer.setpointY = positionContainer.setpointY * setpointAlignmentFactor
                + positionContainer.setpointAbsY * (1 - setpointAlignmentFactor);
        miniPIDX.setSetpoint(positionContainer.setpointX);
        miniPIDY.setSetpoint(positionContainer.setpointY);

        // If landing is enabled, position is not predicted and landing conditions are met
        if (landingEnabled && positionContainer.status != 3
                && Math.abs(positionContainer.x - positionContainer.setpointAbsX) < allowedLandingRangeXY
                && Math.abs(positionContainer.y - positionContainer.setpointAbsY) < allowedLandingRangeXY
                && Math.abs(positionContainer.yaw - positionContainer.setpointYaw) < allowedLandingRangeYaw) {
            // Slowly lowering the altitude
            if (positionContainer.setpointZ > 1)
                positionContainer.setpointZ -= landingDecrement;
            miniPIDZ.setSetpoint(positionContainer.setpointZ);
            positionContainer.status = 2;
        }

        // Append corrections from PID controllers
        positionContainer.ddcX += miniPIDX.getOutput(positionContainer.x);
        positionContainer.ddcY += miniPIDY.getOutput(positionContainer.y);
        positionContainer.ddcZ += miniPIDZ.getOutput(positionContainer.z);
        positionContainer.ddcYaw += miniPIDYaw.getOutput(positionContainer.yaw);

        TransmitPosition(1);
    }

    /**
     * Send corrections to the drone via Serial or UDP
     * @param serviceInfo Service Info for drone
     * 0 - Nothing to do, 1 - Stabilization, 2 - Landing (command not implemented), 3 - Disable motors
     */
    public void TransmitPosition(int serviceInfo) {
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
        // Service info
        directControlData[8] = (byte) (serviceInfo & 0xFF);

        // Check byte
        byte checkByte = 0;
        for (int i = 0; i <= 8; i++)
            checkByte = (byte) (checkByte ^ directControlData[i]);
        directControlData[9] = checkByte;

        // Transmit data
        serialHandler.linkData = directControlData;
        udpHandler.udpData = directControlData;
        serialHandler.pushLinkData();
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
        if (positionContainer.status == 1 || positionContainer.status == 2) {
            // Filter position if status is STAB or LAND
            this.positionContainer.x = this.positionContainer.x * inputFilter + x * (1 - inputFilter);
            this.positionContainer.y = this.positionContainer.y * inputFilter + y * (1 - inputFilter);
            this.positionContainer.z = this.positionContainer.z * inputFilter + z * (1 - inputFilter);
            this.positionContainer.yaw = this.positionContainer.yaw * inputFilter + yaw * (1 - inputFilter);
        } else {
            // Don't filter in other modes (reset position memory)
            this.positionContainer.x = x;
            this.positionContainer.y = y;
            this.positionContainer.z = z;
            this.positionContainer.yaw = yaw;
        }

        // Update status
        if (!landingEnabled || positionContainer.status != 5) {
            // If not landed
            if (stabEnabled)
                positionContainer.status = 1;
            else
                positionContainer.status = 0;
        }
        // Reset lost counter
        lostCounter = 0;

        // Update new position
        proceedPosition();

        // Update status
        statusLast = positionContainer.status;
        platformContainer.status = positionContainer.status;

        // OSD
        proceedOSD();

        // Blackbox
        proceedBlackbox();
    }

    /**
     * Calls when no position estimated
     */
    public void noMarker() {
        switch (positionContainer.status) {
            case 1:
            case 2:
                // STAB
                logger.warn("The marker is lost! " +
                        "The previous position will be used for next " + lostFrames + " frames!");
                lostCounter++;
                positionContainer.status = 3;
                break;
            case 3:
                // PREV
                lostCounter++;
                if (lostCounter >= lostFrames) {
                    logger.error("The marker is completely lost! Stabilization will be terminated!");
                    positionContainer.ddcX = 1500;
                    positionContainer.ddcY = 1500;
                    positionContainer.ddcZ = 1500;
                    positionContainer.ddcYaw = 1500;
                    logger.error("Sending 0-corrections.");
                    TransmitPosition(1);
                    positionContainer.status = 4;
                }
            default:
                break;
        }
        proceedPosition();
        // Update status
        statusLast = positionContainer.status;
        platformContainer.status = positionContainer.status;

        // OSD
        proceedOSD();

        // Blackbox
        proceedBlackbox();
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
        osdFramesCounter++;
        if (osdFramesCounter == pushOSDAfterFrames) {
            osdHandler.newPositionFlag = true;
            osdFramesCounter = 0;
        }
    }

    /**
     * Sets newPosition flag in BlackboxHandler class
     */
    private void proceedBlackbox() {
        blackboxHandler.newPositionFlag = true;
    }

    /**
     * Load variables from json settings
     * @param settings JsonObject loaded from settings file
     */
    public void loadSettings(JsonObject settings) {
        inputFilter = settings.get("input_filter").getAsDouble();
        setpointAlignmentFactor = settings.get("setpoint_alignment_factor").getAsDouble();
        landingDecrement = settings.get("landing_decrement").getAsDouble();
        allowedLandingRangeXY = settings.get("allowed_landing_range_xy").getAsDouble();
        allowedLandingRangeYaw = settings.get("allowed_landing_range_yaw").getAsDouble();
        lostFrames = settings.get("allowed_lost_frames").getAsInt();
        pushOSDAfterFrames = settings.get("push_osd_after_frames").getAsInt();
        directControlData[10] = settings.get("data_suffix_1").getAsString().getBytes()[0];
        directControlData[11] = settings.get("data_suffix_2").getAsString().getBytes()[0];
        positionContainer.setSetpoints(settings.get("setpoint_x").getAsDouble(),
                settings.get("setpoint_y").getAsDouble(),
                0, settings.get("setpoint_yaw").getAsDouble());
        miniPIDX.setSetpoint(positionContainer.setpointX);
        miniPIDY.setSetpoint(positionContainer.setpointY);
        miniPIDZ.setSetpoint(positionContainer.setpointZ);
        miniPIDYaw.setSetpoint(positionContainer.setpointYaw);
    }

    /**
     * Sets coefficients from json
     * @param pids JsonObject pids file (default: pid.json)
     */
    public void setPIDFromJson(JsonObject pids) {
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
}
