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
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.text.DecimalFormat;

public class PlatformHandler implements Runnable {
    private final Logger logger = Logger.getLogger(this.getClass().getSimpleName());
    private final SettingsContainer settingsContainer;
    private final DecimalFormat decimalFormat = new DecimalFormat("#.##");
    private final PlatformContainer platformContainer;
    private final PositionContainer positionContainer;
    private final SerialHandler serialHandler;
    private final VideoCapture videoCapture;
    private long platformLastPacketTime;
    private boolean cycleError;
    private boolean handleRunning;
    private double exposureLast = 0;
    private boolean lightsLast = false;
    private volatile boolean opencvStarts = false;

    /**
     * This class communicates with the platform via the serial port
     * @param platformContainer variables container
     * @param serialHandler serial ports handler
     * @param videoCapture opencv camera capture (for exposure setup)
     */
    PlatformHandler(PlatformContainer platformContainer,
                    PositionContainer positionContainer,
                    SerialHandler serialHandler,
                    VideoCapture videoCapture,
                    SettingsContainer settingsContainer) {
        this.platformContainer = platformContainer;
        this.positionContainer = positionContainer;
        this.serialHandler = serialHandler;
        this.videoCapture = videoCapture;
        this.settingsContainer = settingsContainer;
    }

    /**
     * Sets openCV status
     */
    public void setOpencvStarts(boolean opencvStarts) {
        this.opencvStarts = opencvStarts;
    }

    /**
     * Starts the main loop
     */
    @Override
    @SuppressWarnings("StatementWithEmptyBody")
    public void run() {
        // Wait for Platform
        logger.info("Waiting for platform");
        waitForReplay(-1);
        logger.info("Platform replied successfully");

        // Wait for OpenCVHandler class
        logger.info("Waiting for OpenCVHandler");
        while (!opencvStarts);
        logger.info("Starting main loop");

        // Set loop flag
        handleRunning = true;
        while (handleRunning)
            platformLoop();
    }

    /**
     * Main platform loop
     */
    private void platformLoop() {
        try {
            // Check lost status
            if (!platformContainer.platformLost &&
                    System.currentTimeMillis() - platformLastPacketTime >= settingsContainer.platformLostTime) {
                logger.warn("Platform telemetry lost!");
                platformContainer.platformLost = true;
            }

            cycleError = false;
            if (checkStatus()) {
                readPressureAndGPS();
                illuminationController();
                speedController();
                writeStatus();
            }

            // If no errors
            if (!cycleError) {
                platformLastPacketTime = System.currentTimeMillis();
                platformContainer.platformLost = false;
            }

            // Min loop time
            Thread.sleep(settingsContainer.platformLoopTimer);
        } catch (Exception e) {
            logger.error("Error interacting with platform!", e);
        }
    }

    private boolean checkStatus() {
        // Send L0 to check illumination
        serialHandler.setPlatformData("L8\n".getBytes());
        serialHandler.pushPlatformData();

        // Wait for data
        String incoming = waitForReplay(settingsContainer.platformReplyTimeout);

        // If error
        if (parseNumberFromGCode(incoming, 'S', -1) == 0) {
            // Increment packets counter
            platformContainer.packetsNumber++;
            return true;
        }

        logger.error("Error reading platform status!");
        cycleError = true;
        return false;
    }

    /**
     * Checks current illumination, changes exposure, enables / disables light if needed
     */
    private void illuminationController() {
        // Send L0 to check illumination
        serialHandler.setPlatformData("L0\n".getBytes());
        serialHandler.pushPlatformData();

        // Wait for data
        String incoming = waitForReplay(settingsContainer.platformReplyTimeout);

        // If error
        if (parseNumberFromGCode(incoming, 'S', -1) != 0) {
            logger.error("Error reading illumination!");
            return;
        }

        // Parse illumination
        platformContainer.illumination = (int) parseNumberFromGCode(incoming, 'L', 0);

        // Enable or disable additional light
        if (!lightsLast && platformContainer.illumination < settingsContainer.platformLightEnableThreshold) {
            // Enable Light
            enableLight();
        } else if (lightsLast && platformContainer.illumination > settingsContainer.platformLightDisableThreshold) {
            // Disable Light
            disableLight();
        }

        // Calculate exposure
        platformContainer.cameraExposure = -(Math.exp(((double)platformContainer.illumination - 760.0) / 130.0) + 8.0);

        // Check if need to set a new exposure
        if (Math.abs(platformContainer.cameraExposure - exposureLast) > 0.2) {
            exposureLast = platformContainer.cameraExposure;
            logger.info("Changing exposure to " + decimalFormat.format(platformContainer.cameraExposure));
            videoCapture.set(Videoio.CAP_PROP_EXPOSURE, platformContainer.cameraExposure);
        }
    }

    /**
     * Checks current speed
     */
    private void speedController() {
        // Send L1 to check speed
        serialHandler.setPlatformData("L1\n".getBytes());
        serialHandler.pushPlatformData();

        // Wait for data
        String incoming = waitForReplay(settingsContainer.platformReplyTimeout);

        // If error
        if (parseNumberFromGCode(incoming, 'S', -1) != 0) {
            logger.error("Error reading speed!");
            return;
        }

        // Parse illumination
        platformContainer.speed = parseNumberFromGCode(incoming, 'L', 0);
    }

    /**
     * Reads current platform pressure in PA and GPS lat and lon
     */
    private void readPressureAndGPS() {
        // Read pressure
        serialHandler.setPlatformData("L3\n".getBytes());
        serialHandler.pushPlatformData();
        String incoming = waitForReplay(settingsContainer.platformReplyTimeout);
        if (parseNumberFromGCode(incoming, 'S', -1) != 0) {
            logger.error("Error reading pressure!");
            cycleError = true;
            return;
        }
        platformContainer.pressure = (int) parseNumberFromGCode(incoming, 'P', 0);

        // Read GPS
        serialHandler.setPlatformData("L4\n".getBytes());
        serialHandler.pushPlatformData();
        incoming = waitForReplay(settingsContainer.platformReplyTimeout);
        if (parseNumberFromGCode(incoming, 'S', -1) != 0) {
            logger.error("Error reading GPS!");
            cycleError = true;
            return;
        }
        platformContainer.gpsLatInt = (int) parseNumberFromGCode(incoming, 'A', 0);
        platformContainer.gpsLonInt = (int) parseNumberFromGCode(incoming, 'O', 0);

        platformContainer.gpsLatDouble = platformContainer.gpsLatInt / 1000000.0;
        platformContainer.gpsLonDouble = platformContainer.gpsLonInt / 1000000.0;

        platformContainer.satellitesNum = (int) parseNumberFromGCode(incoming, 'N', 0);
        platformContainer.fixType = (int) parseNumberFromGCode(incoming, 'F', 0);
    }

    /**
     * Writes current status to the platform
     */
    private void writeStatus() {
        // Send L2 to set the status
        serialHandler.setPlatformData(("L2 S" + positionContainer.status + "\n").getBytes());
        serialHandler.pushPlatformData();

        // Wait for data
        waitForReplay(settingsContainer.platformReplyTimeout);
    }

    /**
     * Enables platform light
     */
    private void enableLight() {
        logger.info("Enabling light");
        serialHandler.setPlatformData("M3\n".getBytes());
        // Send data via serial
        serialHandler.pushPlatformData();
        // Wait for complete
        waitForReplay(settingsContainer.platformReplyTimeout);
        // Set flag
        lightsLast = true;
    }

    /**
     * Disables platform light
     */
    private void disableLight() {
        logger.info("Disabling light");
        serialHandler.setPlatformData("M5\n".getBytes());
        // Send data via serial
        serialHandler.pushPlatformData();
        // Wait for complete
        waitForReplay(settingsContainer.platformReplyTimeout);
        // Set flag
        lightsLast = false;
    }

    /**
     * Waits and returns data from the platform
     * @return replay from the platform without '>' symbol
     */
    private String waitForReplay(int timeout) {
        StringBuilder stringBuilder = new StringBuilder();
        long timeStart = System.currentTimeMillis();
        while (stringBuilder.indexOf(">") < 0) {
            stringBuilder.append(new String(serialHandler.readDataFromPlatform()));
            if (timeout >= 0 && System.currentTimeMillis() - timeStart > timeout) {
                logger.error("Timeout waiting for a response from the platform!");
                cycleError = true;
                break;
            }
        }
        return stringBuilder.toString()
                .replace(">", "")
                .replace("\r", "")
                .replace("\n", "");
    }

    /**
     * Parses GCode string
     * @param data GCode string
     * @param code Symbol you want to know the meaning of
     * @param defaultValue Value if the character is not found
     * @return Value of the character
     */
    private double parseNumberFromGCode(String data, char code, float defaultValue) {
        try {
            // Remove endings and spaces
            data = data.replace("\n", "").replace("\r", "").trim();
            int startIndex = data.indexOf(code);
            if (startIndex >= 0) {
                // Trim until next space
                int stopIndex = data.substring(startIndex).indexOf(' ');
                if (stopIndex > 0) {
                    return Double.parseDouble(data.substring(startIndex).substring(1, stopIndex));
                } else
                    return Double.parseDouble(data.substring(startIndex).substring(1));
            }
        } catch (Exception e) {
            logger.error("Error parsing G-Code!", e);
            cycleError = true;
        }
        return defaultValue;
    }

    /**
     * Stops the main loop and disables platform lights
     */
    public void stop() {
        logger.warn("Stopping platform handler");
        handleRunning = false;
        disableLight();
    }
}
