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

import org.apache.log4j.Logger;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.text.DecimalFormat;

public class PlatformHandler implements Runnable {
    private final Logger logger = Logger.getLogger(this.getClass().getSimpleName());
    private final DecimalFormat decimalFormat = new DecimalFormat("#.##");
    private final PlatformContainer platformContainer;
    private final SerialHandler serialHandler;
    private final VideoCapture videoCapture;
    private int timeout, loopTimer, lightEnableThreshold, lightDisableThreshold;
    private boolean handleRunning;
    private double exposureLast = 0;
    private boolean lightsLast = false;
    volatile public boolean opencvStarts = false;

    /**
     * This class communicates with the platform via the serial port
     * @param platformContainer variables container
     * @param serialHandler serial ports handler
     * @param videoCapture opencv camera capture (for exposure setup)
     */
    PlatformHandler(PlatformContainer platformContainer,
                    SerialHandler serialHandler,
                    VideoCapture videoCapture) {
        this.platformContainer = platformContainer;
        this.serialHandler = serialHandler;
        this.videoCapture = videoCapture;
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
     * Loads variables from JSON settings
     */
    public void loadFromSettings() {
        timeout = Main.settings.get("platform_reply_timeout").getAsInt();
        loopTimer = Main.settings.get("platform_loop_timer").getAsInt();
        lightEnableThreshold = Main.settings.get("platform_light_enable_threshold").getAsInt();
        lightDisableThreshold = Main.settings.get("platform_light_disable_threshold").getAsInt();
    }

    /**
     * Stops the main loop and disables platform lights
     */
    public void stop() {
        handleRunning = false;
        disableLight();
    }

    /**
     * Main platform loop
     */
    private void platformLoop() {
        try {
            illuminationController();
            speedController();
            writeStatus();
            Thread.sleep(loopTimer);
        } catch (Exception e) {
            logger.error("Error interacting with platform!", e);
        }
    }

    /**
     * Checks current illumination, changes exposure, enables / disables light if needed
     */
    private void illuminationController() {
        // Send L0 to check illumination
        serialHandler.platformData = "L0\n".getBytes();
        serialHandler.pushPlatformData();

        // Wait for data
        String incoming = waitForReplay(timeout);

        // If error
        if (parseNumberFromGCode(incoming, 'S', -1) != 0) {
            logger.error("Error reading illumination!");
            return;
        }

        // Parse illumination
        platformContainer.illumination = (int) parseNumberFromGCode(incoming, 'L', 0);

        // Enable or disable additional light
        if (!lightsLast && platformContainer.illumination < lightEnableThreshold) {
            // Enable Light
            enableLight();
        } else if (lightsLast && platformContainer.illumination > lightDisableThreshold) {
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
        serialHandler.platformData = "L1\n".getBytes();
        serialHandler.pushPlatformData();

        // Wait for data
        String incoming = waitForReplay(timeout);

        // If error
        if (parseNumberFromGCode(incoming, 'S', -1) != 0) {
            logger.error("Error reading speed!");
            return;
        }

        // Parse illumination
        platformContainer.speed = parseNumberFromGCode(incoming, 'L', 0);
    }

    /**
     * Writes current status to the platform
     */
    private void writeStatus() {
        // Send L2 to set the status
        serialHandler.platformData = ("L2 S" + platformContainer.status + "\n").getBytes();
        serialHandler.pushPlatformData();

        // Wait for data
        waitForReplay(timeout);
    }

    /**
     * Enables platform light
     */
    private void enableLight() {
        logger.info("Enabling light");
        serialHandler.platformData = "M3\n".getBytes();
        // Send data via serial
        serialHandler.pushPlatformData();
        // Wait for complete
        waitForReplay(timeout);
        // Set flag
        lightsLast = true;
    }

    /**
     * Disables platform light
     */
    private void disableLight() {
        logger.info("Disabling light");
        serialHandler.platformData = "M5\n".getBytes();
        // Send data via serial
        serialHandler.pushPlatformData();
        // Wait for complete
        waitForReplay(timeout);
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
        }
        return defaultValue;
    }
}
