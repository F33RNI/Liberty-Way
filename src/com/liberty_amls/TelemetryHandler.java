/*
 * Copyright (C) 2021 Frey Hertz (Pavel Neshumov), Liberty-Way Landing System Project
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

import org.apache.log4j.Logger;

public class TelemetryHandler implements Runnable {
    private final Logger logger = Logger.getLogger(this.getClass().getSimpleName());
    private final TelemetryContainer telemetryContainer;
    private final SerialHandler serialHandler;
    private final UDPHandler udpHandler;
    private final SettingsContainer settingsContainer;
    private final byte[] telemetryBuffer = new byte[30];
    private byte telemetryBytePrevious = 0;
    private int telemetryBufferPosition = 0;
    private long telemetryLastPacketTime = 0;
    private volatile boolean handleRunning;
    private int lastGPSLat = 0, lastGPSLon = 0;

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
        handleRunning = true;
        while (handleRunning)
            telemetryLoop();
    }

    private void telemetryLoop() {
        // Check lost status
        if (!telemetryContainer.telemetryLost &&
                System.currentTimeMillis() - telemetryLastPacketTime >= settingsContainer.telemetryLostTime) {
            logger.warn("Drone telemetry lost!");
            telemetryContainer.telemetryLost = true;
        }

        // Read and parse data from Liberty-Link or UDP port
        byte[] tempBuffer = serialHandler.readDataFromLink();
        if (tempBuffer != null && tempBuffer.length > 0) {
            for (byte tempByte : tempBuffer)
                readAndParse(tempByte);
        } else if (udpHandler.isUdpPortOpened()) {
            readAndParse(udpHandler.takeSingleByte());
        }

        // Calculate speed of the drone
        calculateSpeed();
    }

    private void readAndParse(byte data) {
        telemetryBuffer[telemetryBufferPosition] = data;
        if (telemetryBytePrevious == settingsContainer.dataSuffix1 &&
                telemetryBuffer[telemetryBufferPosition] == settingsContainer.dataSuffix2) {
            // If data suffix appears
            // Reset buffer position
            telemetryBufferPosition = 0;

            // Reset check sum
            byte telemetryCheckByte = 0;

            // Calculate check sum
            for (int i = 0; i <= 26; i++)
                telemetryCheckByte ^= telemetryBuffer[i];

            if (telemetryCheckByte == telemetryBuffer[27]) {
                // Parse data if the checksums are equal

                // Error status
                telemetryContainer.errorStatus = (int) telemetryBuffer[0] & 0xFF;

                // Flight mode
                telemetryContainer.flightMode = (int) telemetryBuffer[1] & 0xFF;

                // Battery voltage
                telemetryContainer.batteryVoltage = (double) (((int) telemetryBuffer[2] & 0xFF)) / 10.0;

                // Temperature
                telemetryContainer.temperature = (short)(((short) telemetryBuffer[4] & 0xFF)
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

                // Number of GPS satellites
                telemetryContainer.satellitesNum = ((int) telemetryBuffer[16] & 0xFF);

                // Fix type of GPS
                telemetryContainer.fixType = ((int) telemetryBuffer[17] & 0xFF);

                this.lastGPSLat = telemetryContainer.gpsLatInt;
                this.lastGPSLon = telemetryContainer.gpsLonInt;

                // GPS Latitude
                telemetryContainer.gpsLatInt = ((int) telemetryBuffer[21] & 0xFF)
                        | ((int) telemetryBuffer[20] & 0xFF) << 8
                        | ((int) telemetryBuffer[19] & 0xFF) << 16
                        | ((int) telemetryBuffer[18] & 0xFF) << 24;

                // GPS Longitude
                telemetryContainer.gpsLonInt = ((int) telemetryBuffer[25] & 0xFF)
                        | ((int) telemetryBuffer[24] & 0xFF) << 8
                        | ((int) telemetryBuffer[23] & 0xFF) << 16
                        | ((int) telemetryBuffer[22] & 0xFF) << 24;

                // Convert GPS position to double
                telemetryContainer.gpsLatDouble = telemetryContainer.gpsLatInt / 1000000.0;
                telemetryContainer.gpsLonDouble = telemetryContainer.gpsLonInt / 1000000.0;

                // Liberty Way sequence step
                telemetryContainer.linkWaypointStep = ((int) telemetryBuffer[26] & 0xFF) % 10;

                // New altitude / gps waypoint flags
                switch (((int) telemetryBuffer[26] & 0xFF) / 10) {
                    case 1:
                        telemetryContainer.linkNewWaypointAltitude = true;
                        telemetryContainer.linkNewWaypointGPS = false;
                        break;
                    case 2:
                        telemetryContainer.linkNewWaypointAltitude = false;
                        telemetryContainer.linkNewWaypointGPS = true;
                        break;
                    case 3:
                        telemetryContainer.linkNewWaypointAltitude = true;
                        telemetryContainer.linkNewWaypointGPS = true;
                        break;
                    default:
                        telemetryContainer.linkNewWaypointAltitude = false;
                        telemetryContainer.linkNewWaypointGPS = false;
                        break;
                }

                // Increment packets counter
                telemetryContainer.packetsNumber++;

                // Reset timer and lost flag
                if (telemetryContainer.telemetryLost)
                    logger.info("Drone telemetry restored");
                telemetryContainer.telemetryLost = false;
                telemetryLastPacketTime = System.currentTimeMillis();
            } else
                logger.warn("Wrong telemetry checksum!");
        } else {
            // Store data bytes
            telemetryBytePrevious = telemetryBuffer[telemetryBufferPosition];
            telemetryBufferPosition++;

            // Reset buffer on overflow
            if (telemetryBufferPosition > 29)
                telemetryBufferPosition = 0;
        }
    }

    public void stop() {
        logger.warn("Turning off drone telemetry reading");
        handleRunning = false;
    }

    /**
     * This method calculates current drone's velocity in km/h
     */
    private void calculateSpeed(){
        long loopTime = System.currentTimeMillis() - telemetryLastPacketTime;

        double distance = Math.sin((telemetryContainer.gpsLatInt - this.lastGPSLat) * Math.PI / 180 / 2.0) *
                Math.sin((telemetryContainer.gpsLatInt - this.lastGPSLat) * Math.PI / 180 / 2.0) +
                Math.sin((telemetryContainer.gpsLonInt - this.lastGPSLon) * Math.PI / 180 / 2.0) *
                Math.sin((telemetryContainer.gpsLonInt - this.lastGPSLon) * Math.PI / 180 / 2.0) *
                Math.cos(telemetryContainer.gpsLatInt) *
                Math.cos(this.lastGPSLat);

        distance = Math.atan2(Math.sqrt(distance), Math.sqrt(1 - distance));

        double speedX;
        double speedY;

        if (loopTime == 0)
            speedX = speedY = 0.0;
        else {
            speedX = settingsContainer.planetRadius * 2 * distance / loopTime;

            speedY = settingsContainer.planetRadius * 2 * distance / loopTime;
        }

        speedX *= 3600;
        speedY *= 3600;

        telemetryContainer.speed = Math.sqrt(speedX * speedX + speedY * speedY);
    }
}
