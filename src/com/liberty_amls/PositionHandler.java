/*
 * Copyright (C) 2021 Fern H. (aka Pavel Neshumov), Liberty-Way Landing System Project
 * This software is part of Liberty Drones Project aka AMLS (Autonomous Multirotor Landing System)
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
 *
 * IT IS STRICTLY PROHIBITED TO USE THE PROJECT (OR PARTS OF THE PROJECT / CODE)
 * FOR MILITARY PURPOSES. ALSO, IT IS STRICTLY PROHIBITED TO USE THE PROJECT (OR PARTS OF THE PROJECT / CODE)
 * FOR ANY PURPOSE THAT MAY LEAD TO INJURY, HUMAN, ANIMAL OR ENVIRONMENTAL DAMAGE.
 * ALSO, IT IS PROHIBITED TO USE THE PROJECT (OR PARTS OF THE PROJECT / CODE) FOR ANY PURPOSE THAT
 * VIOLATES INTERNATIONAL HUMAN RIGHTS OR HUMAN FREEDOM.
 * BY USING THE PROJECT (OR PART OF THE PROJECT / CODE) YOU AGREE TO ALL OF THE ABOVE RULES.
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
    private final WaypointsContainer waypointsContainer;

    private int waypointSendIndex = 0;
    private int lostCounter = 0;
    private boolean libertyWayEnabled = false;
    private final GPS emptyGPS;
    private String preFlightErrorMessage = "";
    private boolean takeoffDetectedLast = false;

    /**
     * This class takes the absolute coordinates of the marker as input,
     * passes them through the PID controllers,
     * generates Direct Control values and sends them to the drone via the Serial port or UDP
     *
     * @param linkSender LinkSender class object to send data
     */
    public PositionHandler(LinkSender linkSender,
                           PositionContainer positionContainer,
                           PlatformContainer platformContainer,
                           TelemetryContainer telemetryContainer,
                           BlackboxHandler blackboxHandler,
                           SettingsContainer settingsContainer,
                           WaypointsContainer waypointsContainer) {
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
        this.waypointsContainer = waypointsContainer;

        this.emptyGPS = new GPS(0, 0);

        positionContainer.setSetpoints(settingsContainer.setpointX, settingsContainer.setpointY, 0,
                settingsContainer.setpointYaw);
        miniPIDX.setSetpoint(positionContainer.setpointX);
        miniPIDY.setSetpoint(positionContainer.setpointY);
        miniPIDZ.setSetpoint(positionContainer.setpointZ);
        miniPIDYaw.setSetpoint(positionContainer.setpointYaw);
    }

    /**
     * Calls if there is no marker in sight (newMarkerPosition = false)
     *
     * @param newMarkerPosition must be false
     */
    public void proceedPosition(boolean newMarkerPosition) {
        proceedPosition(newMarkerPosition, 0, 0, 0, 0);
    }

    /**
     * Calls by OpenCV handler
     *
     * @param newMarkerPosition set it to true if marker is in sight
     * @param x                 estimated marker X position (if newMarkerPosition)
     * @param y                 estimated marker Y position (if newMarkerPosition)
     * @param z                 estimated marker Z position (if newMarkerPosition)
     * @param yaw               estimated marker Yaw angle (if newMarkerPosition)
     */
    public void proceedPosition(boolean newMarkerPosition, double x, double y, double z, double yaw) {
        // Set starting DDC values (1500 = no correction)
        positionContainer.ddcX = 1500;
        positionContainer.ddcY = 1500;
        positionContainer.ddcZ = 1500;
        positionContainer.ddcYaw = 1500;

        // Find distance between drone and platform
        if (!platformContainer.platformLost && platformContainer.gps.getSatellitesNum() > 0
                && !telemetryContainer.telemetryLost && telemetryContainer.gps.getSatellitesNum() > 0)
            positionContainer.distance = (int) GPS.distanceOnGeoid(telemetryContainer.gps,
                    platformContainer.gps, settingsContainer.planetRadius);

        // TODO: In-flight error checking

        switch (positionContainer.status) {
            case PositionContainer.STATUS_WAYP:
                // ---------------------------------------------
                // WAYP - Broadcasting waypoints array
                // ---------------------------------------------
                // Send waypoints array
                sendWaypoints();

                // Finish the flight if the drone landed on its own
                if (!telemetryContainer.takeoffDetected && takeoffDetectedLast) {
                    positionContainer.status = PositionContainer.STATUS_DONE;
                }
                // Normal mode
                else {
                    // Send takeoff command if end of array is reached
                    if (waypointSendIndex >= WaypointsContainer.WAYPOINTS_NUM) {
                        if (!telemetryContainer.telemetryLost) {
                            if (!telemetryContainer.takeoffDetected)
                                linkSender.sendTakeoff();
                        } else
                            linkSender.sendTakeoff();
                    }

                    // Reset optical PID controllers
                    resetPIDs();

                    // Look for marker only if drone telemetry is lost or DDC is allowed on current waypoint
                    if (telemetryContainer.telemetryLost
                            || (telemetryContainer.waypointIndex < waypointsContainer.getWaypointsSize()
                            && waypointsContainer.getWaypointsCommand()
                            .get(telemetryContainer.waypointIndex) < WaypointsContainer.WAYP_CMD_BITS_FLY)) {

                        // If the marker was found
                        if (newMarkerPosition && z <= settingsContainer.maxMarkerHeight) {
                            // Reset filtered values
                            this.positionContainer.x = x;
                            this.positionContainer.y = y;
                            this.positionContainer.z = z;
                            this.positionContainer.yaw = yaw;

                            // Remember current position as setpoint
                            positionContainer.setpointX = positionContainer.x;
                            positionContainer.setpointY = positionContainer.y;
                            positionContainer.setpointZ = positionContainer.z;
                            positionContainer.entryZ = positionContainer.z;
                            miniPIDX.setSetpoint(positionContainer.setpointX);
                            miniPIDY.setSetpoint(positionContainer.setpointY);
                            miniPIDZ.setSetpoint(positionContainer.setpointZ);

                            // Print log message
                            logger.warn("Marker in sight! Setpoints fixed at X=" +
                                    (int) positionContainer.setpointX +
                                    " Y=" + (int) positionContainer.setpointY +
                                    " Z=" + (int) positionContainer.setpointZ);
                            logger.warn("Start smooth alignment of setpoints");

                            // Switch to the STAB mode (optical stabilization)
                            positionContainer.status = PositionContainer.STATUS_STAB;
                        }
                    }
                }
                break;

            case PositionContainer.STATUS_STAB:
                // ---------------------------------------------
                // STAB - Optical stabilization
                // ---------------------------------------------
                // Reset lost frames counter
                lostCounter = 0;

                // Proceed optical stabilization
                opticalStabilization(x, y, z, yaw);

                // Switch to PREV mode if marker was lost
                if (!newMarkerPosition || z > settingsContainer.maxMarkerHeight) {
                    logger.warn("The marker is lost! The previous position will be used for next " +
                            settingsContainer.allowedLostFrames + " frames");
                    lostCounter++;
                    positionContainer.status = PositionContainer.STATUS_PREV;
                } else {
                    // Switch to LAND mode if landing conditions are met
                    if (settingsContainer.opticalLandingAllowed
                            && Math.abs(positionContainer.x - positionContainer.setpointAbsX) <
                            settingsContainer.allowedLandingRangeXY
                            && Math.abs(positionContainer.y - positionContainer.setpointAbsY) <
                            settingsContainer.allowedLandingRangeXY
                            && Math.abs(positionContainer.yaw - positionContainer.setpointYaw) <
                            settingsContainer.allowedLandingRangeYaw)
                        positionContainer.status = PositionContainer.STATUS_LAND;
                }

                // Log new data
                blackboxHandler.requestNewEntry();
                break;
            case PositionContainer.STATUS_LAND:
                // ---------------------------------------------
                // LAND - Optical landing
                // ---------------------------------------------
                // Switch to PREV mode if marker was lost
                if (!newMarkerPosition || z > settingsContainer.maxMarkerHeight) {
                    logger.warn("The marker is lost! The previous position will be used for next " +
                            settingsContainer.allowedLostFrames + " frames");
                    lostCounter++;
                    positionContainer.status = PositionContainer.STATUS_PREV;
                } else {
                    if (positionContainer.z <= settingsContainer.motorsTurnOffHeight) {
                        // Landing done
                        logger.warn("Landed successfully! Turning off the motors");
                        linkSender.sendMotorsOFF();
                        if (settingsContainer.isTelemetryNecessary && !telemetryContainer.telemetryLost) {
                            if (!telemetryContainer.takeoffDetected)
                                // Switch to DONE state if the drone has landed
                                positionContainer.status = PositionContainer.STATUS_DONE;
                        } else
                            // TODO: detect drone landing using platform
                            positionContainer.status = PositionContainer.STATUS_DONE;
                    } else {
                        // Landing in process
                        // Check landing conditions
                        if (Math.abs(positionContainer.x - positionContainer.setpointAbsX) <
                                settingsContainer.allowedLandingRangeXY
                                && Math.abs(positionContainer.y - positionContainer.setpointAbsY) <
                                settingsContainer.allowedLandingRangeXY
                                && Math.abs(positionContainer.yaw - positionContainer.setpointYaw) <
                                settingsContainer.allowedLandingRangeYaw) {
                            // Slowly lowering the altitude
                            if (positionContainer.setpointZ > 1)
                                positionContainer.setpointZ -= settingsContainer.landingDecrement;
                            miniPIDZ.setSetpoint(positionContainer.setpointZ);
                        } else
                            // If landing conditions are not met, return to STAB mode
                            positionContainer.status = PositionContainer.STATUS_STAB;

                        // Calculate and send direct controls
                        opticalStabilization(x, y, z, yaw);
                    }
                }

                // Log new data
                blackboxHandler.requestNewEntry();
                break;
            case PositionContainer.STATUS_PREV:
                // ---------------------------------------------
                // PREV - Optical stabilization with lost marker
                // ---------------------------------------------
                // Proceed optical stabilization
                opticalStabilization(x, y, z, yaw);

                // Switch to the STAB mode (optical stabilization) if the marker has appeared again
                if (newMarkerPosition && z <= settingsContainer.maxMarkerHeight) {
                    logger.warn("The marker is back in sight!");
                    positionContainer.status = PositionContainer.STATUS_STAB;
                } else {
                    // Increment lost frames counter
                    lostCounter++;
                    if (lostCounter > settingsContainer.allowedLostFrames) {
                        logger.error("The marker is completely lost! Optical stabilization will be terminated!");

                        // Switch to the WAYP mode if marker is completely lost
                        positionContainer.status = PositionContainer.STATUS_WAYP;
                    }
                }

                // Log new data
                blackboxHandler.requestNewEntry();
                break;
            case PositionContainer.STATUS_DONE:
                // ---------------------------------------------
                // DONE - Landing finished
                // ---------------------------------------------
                // Print message
                logger.info("DONE! Liberty-Way sequence finished");

                // Disable blackbox
                blackboxHandler.setBlackboxEnabled(false);

                // Disable Liberty-Way
                this.libertyWayEnabled = false;

                // Switch to the IDLE mode
                positionContainer.status = PositionContainer.STATUS_IDLE;

                // Log new data
                blackboxHandler.requestNewEntry();
                break;
            default:
                // ---------------------------------------------
                // IDLE - Waiting for execution (pre-start)
                // ---------------------------------------------
                // Send waypoints array
                sendWaypoints();
                break;
        }

        // Store takeoff detected flag for the next loop
        takeoffDetectedLast = telemetryContainer.takeoffDetected;
    }

    /**
     * Sends the waypoints array to the drone
     */
    private void sendWaypoints() {
        // Send waypoint
        if (waypointSendIndex < WaypointsContainer.WAYPOINTS_NUM) {
            // Send current waypoint
            if (waypointsContainer.getWaypointsSize() > waypointSendIndex
                    && waypointsContainer.getWaypointsAPI().get(waypointSendIndex) > WaypointsContainer.WAYPOINT_SKIP) {
                linkSender.sendWaypoint(waypointsContainer.getWaypointsGPS().get(waypointSendIndex),
                        waypointsContainer.getWaypointsCommand().get(waypointSendIndex), waypointSendIndex);
            } else
                linkSender.sendWaypoint(emptyGPS, WaypointsContainer.WAYP_CMD_BITS_SKIP, waypointSendIndex);


            // Increment waypoint counter
            waypointSendIndex++;
        }

        // Reset index
        else {
            // Start from the current waypoint or from the 0 if no telemetry or the drone is not in flight
            if (!telemetryContainer.telemetryLost && telemetryContainer.takeoffDetected)
                waypointSendIndex = telemetryContainer.waypointIndex;
            else
                waypointSendIndex = 0;

            // Log new data
            if (positionContainer.status != PositionContainer.STATUS_IDLE)
                blackboxHandler.requestNewEntry();
        }
    }

    /**
     * Processes the coordinates of the marker, calculates optical stabilization
     * and sends Direct Control to the drone
     */
    private void opticalStabilization(double x, double y, double z, double yaw) {
        // Filter new coordinates
        this.positionContainer.x = this.positionContainer.x * settingsContainer.inputFilter +
                x * (1 - settingsContainer.inputFilter);
        this.positionContainer.y = this.positionContainer.y * settingsContainer.inputFilter +
                y * (1 - settingsContainer.inputFilter);
        this.positionContainer.z = this.positionContainer.z * settingsContainer.inputFilter +
                z * (1 - settingsContainer.inputFilter);
        this.positionContainer.yaw = this.positionContainer.yaw * settingsContainer.inputFilter +
                yaw * (1 - settingsContainer.inputFilter);

        // Setpoints alignment
        positionContainer.setpointX = positionContainer.setpointX * settingsContainer.setpointAlignmentFactor
                + positionContainer.setpointAbsX * (1 - settingsContainer.setpointAlignmentFactor);
        positionContainer.setpointY = positionContainer.setpointY * settingsContainer.setpointAlignmentFactor
                + positionContainer.setpointAbsY * (1 - settingsContainer.setpointAlignmentFactor);
        miniPIDX.setSetpoint(positionContainer.setpointX);
        miniPIDY.setSetpoint(positionContainer.setpointY);

        // Append corrections from PID controllers
        positionContainer.ddcX += miniPIDX.getOutput(positionContainer.x);
        positionContainer.ddcY += miniPIDY.getOutput(positionContainer.y);
        positionContainer.ddcZ += miniPIDZ.getOutput(positionContainer.z);
        positionContainer.ddcYaw += miniPIDYaw.getOutput(positionContainer.yaw);

        // Calculate roll and pitch corrections
        double yawSin = Math.sin(Math.toRadians(-positionContainer.yaw));
        double yawCos = Math.cos(Math.toRadians(-positionContainer.yaw));
        positionContainer.ddcRoll = (int) ((positionContainer.ddcX - 1500) * yawSin
                + (positionContainer.ddcY - 1500) * yawCos + 1500);
        positionContainer.ddcPitch = (int) ((positionContainer.ddcX - 1500) * yawCos
                - (positionContainer.ddcY - 1500) * yawSin + 1500);

        // Send direct correction to the drone
        linkSender.sendDDC(positionContainer.ddcRoll, positionContainer.ddcPitch,
                positionContainer.ddcZ, positionContainer.ddcYaw, LinkSender.CMD_BITS_DDC);
    }

    /**
     * Enables or disables main Liberty-Way sequence
     *
     * @param libertyWayEnabled set to tru to start auto-takeoff sequence and flight over-waypoints
     * @return true if requested operation was successful
     */
    public boolean setLibertyWayEnabled(boolean libertyWayEnabled) {
        if (libertyWayEnabled != this.libertyWayEnabled) {
            // Reset waypoints send index
            waypointSendIndex = 0;

            if (this.libertyWayEnabled) {
                // Reset current status to IDLE
                positionContainer.status = PositionContainer.STATUS_IDLE;

                // Disable Liberty-Way
                if (telemetryContainer.takeoffDetected)
                    // Send auto landing command if closed in flight
                    linkSender.sendLand();

                // Disable blackbox
                blackboxHandler.setBlackboxEnabled(false);

                // Disable Liberty-Way
                this.libertyWayEnabled = false;
            } else {
                // Starting Liberty-Way
                if (preFlightChecks()) {
                    // If checks passed set status to WAYP
                    positionContainer.status = PositionContainer.STATUS_WAYP;

                    // Clear takeoffDetectedLast flag
                    takeoffDetectedLast = false;

                    // Enable blackbox
                    blackboxHandler.setBlackboxEnabled(settingsContainer.blackboxEnabled);

                    // Enable Liberty-Way
                    this.libertyWayEnabled = true;

                    // Return true
                    return true;
                } else
                    // Don't enable Liberty-Way if error during pre-flight checks
                    this.libertyWayEnabled = false;
            }
        } else
            return true;
        return false;
    }

    /**
     * Performs a basic system check before starting a Liberty-Way sequence
     *
     * @return true if the checks passed
     */
    private boolean preFlightChecks() {
        boolean checksPassed = true;
        if (positionContainer.status != PositionContainer.STATUS_IDLE)
            checksPassed = preFlightError("Initial status is not IDLE");

        else if (!positionContainer.isFrameNormal)
            checksPassed = preFlightError("Wrong camera frame");

        else if (platformContainer.platformLost)
            checksPassed = preFlightError("No communication with the platform");

        else if (platformContainer.errorStatus != 0)
            checksPassed = preFlightError("Platform error " + platformContainer.errorStatus);

        else if (settingsContainer.isTelemetryNecessary && telemetryContainer.telemetryLost)
            checksPassed = preFlightError("No drone telemetry");

        else if (settingsContainer.isTelemetryNecessary && telemetryContainer.errorStatus != 0)
            checksPassed = preFlightError("Drone error " + telemetryContainer.errorStatus);

        else if (platformContainer.gps.getSatellitesNum() < settingsContainer.minSatellitesNumStart)
            checksPassed = preFlightError("Not enough GPS satellites on the platform");

        else if (platformContainer.gps.getGroundSpeed() > settingsContainer.maxPlatformSpeed)
            checksPassed = preFlightError("The platform moves faster than " +
                    settingsContainer.maxPlatformSpeed + " km/h");

        else if (settingsContainer.isTelemetryNecessary
                && telemetryContainer.gps.getSatellitesNum() < settingsContainer.minSatellitesNumStart)
            checksPassed = preFlightError("Not enough GPS satellites on the drone");

        else if (waypointsContainer.getWaypointsSize() <= 0)
            checksPassed = preFlightError("No waypoints set!");

        else if (waypointsContainer.getWaypointsAPI().get(waypointsContainer.getWaypointsSize() - 1)
                != WaypointsContainer.WAYPOINT_PLATFORM
                && waypointsContainer.getWaypointsAPI().get(waypointsContainer.getWaypointsSize() - 1)
                != WaypointsContainer.WAYPOINT_LAND)
            checksPassed = preFlightError("The last waypoint does not provide a platform or landing. " +
                    "The drone will never land!");

        else {
            int platformCount = 0;
            for (int i = 0; i < waypointsContainer.getWaypointsSize(); i++) {
                if (waypointsContainer.getWaypointsAPI().get(i) == WaypointsContainer.WAYPOINT_PLATFORM)
                    platformCount++;
            }
            if (platformCount > 1)
                checksPassed = preFlightError("More than one platform waypoint specified!");
        }

        // Checks passed
        if (checksPassed) {
            logger.info("Basic pre-flight checks passed");
            logger.warn("CAUTION! Starting LibertyWay sequence! Motor start possible!");
        }
        return checksPassed;
    }

    /**
     * Prints error message during pre-flight checks
     * @param errorMessage error description
     */
    private boolean preFlightError(String errorMessage) {
        logger.error("Error during pre-flight checks! " + errorMessage);
        preFlightErrorMessage = "Error during pre-flight checks! " + errorMessage;
        return false;
    }

    /**
     * @return error message during pre-flight checks
     */
    public String getPreFlightErrorMessage() {
        return preFlightErrorMessage;
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
     *
     * @param pid     JsonObject PID (from file)
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
