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

import org.apache.log4j.Logger;

import java.util.Timer;
import java.util.TimerTask;

public class TelemetryHandler implements Runnable {
    private final Logger logger = Logger.getLogger(this.getClass().getSimpleName());
    private final SettingsContainer settingsContainer;
    private final TelemetryContainer telemetryContainer;
    private final SerialHandler serialHandler;
    private final UDPHandler udpHandler;
    private final byte[] telemetryBuffer = new byte[34];
    private byte telemetryBytePrevious = 0;
    private int telemetryBufferPosition = 0;
    private volatile boolean handleRunning;

    TelemetryHandler(TelemetryContainer telemetryContainer, SerialHandler serialHandler,
                     UDPHandler udpHandler, SettingsContainer settingsContainer) {
        this.telemetryContainer = telemetryContainer;
        this.serialHandler = serialHandler;
        this.udpHandler = udpHandler;
        this.settingsContainer = settingsContainer;
    }

    @Override
    public void run() {
        // Set loop flag
        logger.info("Starting main loop");

        // Initialize timer
        Timer telemetryLostCheckTimer = new Timer("Telemetry lost checking timer");

        // Create task for checking lost status
        TimerTask telemetryLostCheck = new TimerTask() {
            @Override
            public void run() {
                // Check platform lost status
                if (!telemetryContainer.telemetryLost &&
                        System.currentTimeMillis() - telemetryContainer.telemetryLastPacketTime
                                >= settingsContainer.telemetryLostTime) {
                    logger.error("Drone telemetry lost!");
                    telemetryContainer.telemetryLost = true;
                }
            }
        };

        // Start timer
        telemetryLostCheckTimer.scheduleAtFixedRate(telemetryLostCheck, settingsContainer.telemetryLostTime,
                settingsContainer.telemetryLostTime);

        // Start main loop
        handleRunning = true;
        while (handleRunning) {
            try {
                // Read and parse data from UDP or serial port
                if (udpHandler.isUdpPortOpened()) {
                    readAndParse(udpHandler.takeSingleByte());
                } else {
                    readAndParse(serialHandler.takeSingleByte());
                }
            } catch (Exception e) {
                logger.error("Error reading telemetry from the drone!", e);
            }
        }
    }

    private void readAndParse(byte data) {
        telemetryBuffer[telemetryBufferPosition] = data;
        if (telemetryBytePrevious == settingsContainer.droneDataSuffix1
                && telemetryBuffer[telemetryBufferPosition] == settingsContainer.droneDataSuffix2) {
            // If data suffix appears
            // Reset buffer position
            telemetryBufferPosition = 0;

            // Reset check sum
            byte telemetryCheckByte = 0;

            // Calculate check sum
            for (int i = 0; i <= 30; i++)
                telemetryCheckByte ^= telemetryBuffer[i];

            if (telemetryCheckByte == telemetryBuffer[31]) {
                // Parse data if the checksums are equal

                // Error status
                telemetryContainer.errorStatus = (int) telemetryBuffer[0] & 0xFF;

                // Flight mode
                telemetryContainer.flightMode = (int) telemetryBuffer[1] & 0xFF;

                // Battery voltage
                telemetryContainer.batteryVoltage = (double) (((int) telemetryBuffer[2] & 0xFF)) / 10.0;

                // Temperature
                telemetryContainer.temperature = (short) (((short) telemetryBuffer[4] & 0xFF)
                        | ((short) telemetryBuffer[3] & 0xFF) << 8);
                telemetryContainer.temperature = (telemetryContainer.temperature / 340.0) + 36.53;

                // Roll, pitch angles
                telemetryContainer.angleRoll = (int) telemetryBuffer[5] & 0xFF;
                telemetryContainer.angleRoll -= 100;
                telemetryContainer.anglePitch = (int) telemetryBuffer[6] & 0xFF;
                telemetryContainer.anglePitch -= 100;

                // Start status
                telemetryContainer.startStatus = (int) telemetryBuffer[7] & 0xFF;

                // Altitude
                telemetryContainer.altitude = ((int) telemetryBuffer[9] & 0xFF)
                        | ((int) telemetryBuffer[8] & 0xFF) << 8;
                telemetryContainer.altitude -= 1000;

                // Takeoff throttle
                telemetryContainer.takeoffThrottle = ((int) telemetryBuffer[11] & 0xFF)
                        | ((int) telemetryBuffer[10] & 0xFF) << 8;

                // Takeoff detected
                telemetryContainer.takeoffDetected = ((int) telemetryBuffer[12] & 0xFF) > 0;

                // Yaw angle
                telemetryContainer.angleYaw = ((int) telemetryBuffer[14] & 0xFF)
                        | ((int) telemetryBuffer[13] & 0xFF) << 8;

                // Heading lock
                telemetryContainer.headingLock = ((int) telemetryBuffer[15] & 0xFF) > 0;

                // New GPS coordinates
                telemetryContainer.gps.setFromInt(((int) telemetryBuffer[19] & 0xFF)
                        | ((int) telemetryBuffer[18] & 0xFF) << 8
                        | ((int) telemetryBuffer[17] & 0xFF) << 16
                        | ((int) telemetryBuffer[16] & 0xFF) << 24,
                        ((int) telemetryBuffer[23] & 0xFF)
                        | ((int) telemetryBuffer[22] & 0xFF) << 8
                        | ((int) telemetryBuffer[21] & 0xFF) << 16
                        | ((int) telemetryBuffer[20] & 0xFF) << 24);

                // TNumber of GPS satellites
                telemetryContainer.gps.setSatellitesNum((int) telemetryBuffer[24] & 0xFF);

                // Ground speed (from GPS)
                telemetryContainer.gps.setGroundSpeed((((int) telemetryBuffer[26] & 0xFF)
                        | ((int) telemetryBuffer[25] & 0xFF) << 8) / 10.0);

                // Liberty Way sequence step
                if ((int) (telemetryBuffer[27] & 0xFF) < 128) {
                    telemetryContainer.linkWaypointStep = ((int) telemetryBuffer[27] & 0xFF);
                    telemetryContainer.autoLandingStep = 0;
                }

                // Auto-landing step
                else {
                    telemetryContainer.linkWaypointStep = 0;
                    telemetryContainer.autoLandingStep = ((int) telemetryBuffer[27] & 0xFF) - 128;
                }

                // Liberty Way waypoint index
                telemetryContainer.waypointIndex = ((int) telemetryBuffer[28] & 0xFF);

                // Sonarus distance to ground
                telemetryContainer.sonarusDistanceCm = ((int) telemetryBuffer[29] & 0xFF) * 2;

                // Illumination from LUX meter
                telemetryContainer.illumination = ((int) telemetryBuffer[30] & 0xFF) - 1.0;
                if (telemetryContainer.illumination >= 0.0)
                    telemetryContainer.illumination = Math.pow(telemetryContainer.illumination, 2.105);
                else
                    telemetryContainer.illumination = 0;

                // Increment packets counter
                telemetryContainer.packetsNumber++;

                // Reset timer and lost flag
                if (telemetryContainer.telemetryLost)
                    logger.warn("Drone telemetry restored");
                telemetryContainer.telemetryLost = false;
                telemetryContainer.telemetryLastPacketTime = System.currentTimeMillis();
            } else
                logger.warn("Wrong telemetry checksum!");
        } else {
            // Store data bytes
            telemetryBytePrevious = telemetryBuffer[telemetryBufferPosition];
            telemetryBufferPosition++;

            // Reset buffer on overflow
            if (telemetryBufferPosition > 33)
                telemetryBufferPosition = 0;
        }
    }

    public void stop() {
        logger.warn("Turning off drone telemetry reading");
        handleRunning = false;
    }
}
