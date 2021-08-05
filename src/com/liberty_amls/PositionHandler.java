/*
 * Copyright (C) 2021 Fern Hertz (Pavel Neshumov), Liberty-Way Landing System Project
 *
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
 *
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package com.liberty_amls;

import com.google.gson.JsonObject;
import com.stormbots.MiniPID;
import org.apache.log4j.Logger;

public class PositionHandler {
    private final Logger logger = Logger.getLogger(this.getClass().getSimpleName());
    private final SettingsContainer settingsContainer;
    private final LinkSender linkSender;
    private final MiniPID miniPIDX, miniPIDY, miniPIDZ, miniPIDYaw;
    private final PositionContainer positionContainer;
    private final PlatformContainer platformContainer;
    private final TelemetryContainer telemetryContainer;
    private final BlackboxHandler blackboxHandler;
    private int waypointStep = 0;
    private int lostCounter = 0;
    private boolean libertyWayEnabled = false;

    /**
     * This class takes the absolute coordinates of the marker as input,
     * passes them through the PID controllers,
     * generates Direct Control values and sends them to the drone via the Serial port or UDP
     * @param linkSender LinkSender class object to send data
     */
    public PositionHandler(LinkSender linkSender,
                           PositionContainer positionContainer,
                           PlatformContainer platformContainer,
                           TelemetryContainer telemetryContainer,
                           BlackboxHandler blackboxHandler,
                           SettingsContainer settingsContainer) {
        this.linkSender = linkSender;
        this.miniPIDX = new MiniPID(0, 0, 0, 0);
        this.miniPIDY = new MiniPID(0, 0, 0, 0);
        this.miniPIDZ = new MiniPID(0, 0, 0, 0);
        this.miniPIDYaw = new MiniPID(0, 0, 0, 0);
        this.positionContainer = positionContainer;
        this.platformContainer = platformContainer;
        this.telemetryContainer = telemetryContainer;
        this.blackboxHandler = blackboxHandler;
        this.settingsContainer = settingsContainer;

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
        // Find distance between drone and platform
        positionContainer.distance = (int) SpeedHandler.distanceOnGeoid(telemetryContainer.gps,
                platformContainer.gps, settingsContainer.planetRadius);

        // Reset DDC values (1500 = no correction)
        positionContainer.ddcX = 1500;
        positionContainer.ddcY = 1500;
        positionContainer.ddcZ = 1500;
        positionContainer.ddcYaw = 1500;

        // Reset PID controllers if mode is IDLE or not STAB, LAND, PREV
        if (positionContainer.status == 0 || positionContainer.status >= 4)
            resetPIDs();

        // Begin Liberty Way sequence if libertyWayEnabled, IDLE mode and platform and drone are available
        if (!settingsContainer.onlyOpticalStabilization
                && libertyWayEnabled
                && positionContainer.status == 0
                && !telemetryContainer.telemetryLost
                && !platformContainer.platformLost
                && telemetryContainer.errorStatus == 0
                && platformContainer.errorStatus == 0
                && telemetryContainer.satellitesNum >= settingsContainer.minSatellitesNumStart
                && platformContainer.satellitesNum >= settingsContainer.minSatellitesNumStart) {
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
                        linkSender.sendMotorsStop();
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

            // Calculate roll and pitch corrections
            double yawSin = Math.sin(Math.toRadians(-positionContainer.yaw));
            double yawCos = Math.cos(Math.toRadians(-positionContainer.yaw));
            positionContainer.ddcRoll = (int)((positionContainer.ddcX - 1500) * yawSin
                    + (positionContainer.ddcY - 1500) * yawCos + 1500);
            positionContainer.ddcPitch = (int)((positionContainer.ddcX - 1500) * yawCos
                    - (positionContainer.ddcY - 1500) * yawSin + 1500);

            // Send direct correction to the drone
            linkSender.sendDDC(positionContainer.ddcRoll, positionContainer.ddcPitch,
                    positionContainer.ddcZ, positionContainer.ddcYaw);
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
                        linkSender.sendAbort();
                        positionContainer.status = 4;
                    }
                    break;
                case 4:
                    // LOST
                    if (!settingsContainer.onlyOpticalStabilization && !platformContainer.platformLost) {
                        // Enter WAYP loop if optical stabilization has been lost
                        logger.warn("Switching to WAYP mode");
                        linkSender.sendIDLE();
                        positionContainer.status = 6;
                    } else {
                        // Send abort command
                        linkSender.sendAbort();
                    }
                    break;
                case 5:
                    // TKOF
                case 6:
                    // WAYP
                    if (!platformContainer.platformLost) {
                        // If platform connected
                        waypointStep++;
                        if (waypointStep == 1) {
                            // Step 1. Send gps waypoint
                            linkSender.sendGPSWaypoint(platformContainer.gps);
                        } else if (waypointStep == 2) {
                            // Step 2. Send altitude waypoint
                            linkSender.sendPressureWaypoint(platformContainer.pressure);
                        } else if (waypointStep == 3) {
                            // Step 3. Send command to begin Liberty Way sequence (take off)
                            linkSender.sendStartSequence();
                        } else {
                            // Other steps. IDLE state (telemetry receiving)
                            linkSender.sendIDLE();

                            // Reset counter
                            waypointStep = 0;
                        }
                    } else {
                        // If platform lost
                        // Reset counter
                        waypointStep = 0;

                        // Send abort command
                        linkSender.sendAbort();
                    }
                    break;
                default:
                    // Other modes
                    linkSender.sendIDLE();
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
        if (libertyWayEnabled != this.libertyWayEnabled) {
            if (this.libertyWayEnabled) {
                // Disable Liberty-Way
                if (telemetryContainer.takeoffDetected)
                    // Send abort command if closed in flight
                    linkSender.sendAbort();
                else
                    // Send IDLE command in other cases
                    linkSender.sendIDLE();
            } else
                linkSender.sendIDLE();
            // Reset current status to IDLE
            positionContainer.status = 0;
            // Reset waypoints step
            waypointStep = 0;
        }
        this.libertyWayEnabled = libertyWayEnabled;
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
}
