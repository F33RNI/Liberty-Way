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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;

public class BlackboxHandler implements Runnable {
    private final Logger logger = Logger.getLogger(this.getClass().getSimpleName());
    private final DecimalFormat decimalFormat = new DecimalFormat("#.#");
    private final PositionContainer positionContainer;
    private final PlatformContainer platformContainer;
    private final String blackboxDirectory;
    private boolean handlerRunning;
    private boolean fileStarted = false;
    private long timeStart;
    private BufferedWriter bufferedWriter;
    private FileOutputStream fileOutputStream;
    private OutputStreamWriter outputStreamWriter;
    volatile public boolean blackboxEnabled, stabilizationEnabled = false;
    volatile public boolean newPositionFlag = false;

    /**
     * This class writes the current state of the drone to log files
     * @param positionContainer container of the current position
     * @param blackboxDirectory Folder where .csv log files are stored
     */
    BlackboxHandler(PositionContainer positionContainer,
                    PlatformContainer platformContainer,
                    String blackboxDirectory) {
        this.positionContainer = positionContainer;
        this.platformContainer = platformContainer;
        this.blackboxDirectory = blackboxDirectory;
        this.blackboxEnabled = Main.settings.get("blackbox_enabled_by_default").getAsBoolean();
    }

    /**
     * Starts main loop
     */
    @Override
    public void run() {
        handlerRunning = true;
        while (handlerRunning)
            proceedLogs();
    }

    /**
     * Close file stream and end the loop
     */
    public void stop() {
        handlerRunning = false;
        closeFile();
    }

    /**
     * Checks current state and opens/closes file or pushes new data
     */
    private void proceedLogs() {
        if (fileStarted && (!blackboxEnabled || !stabilizationEnabled || positionContainer.status == 5)) {
            // Blackbox disabled or stabilization disabled or status == DONE
            // Close file
            closeFile();
        }
        if (!fileStarted && blackboxEnabled && stabilizationEnabled
                && (positionContainer.status == 1 || positionContainer.status == 2)) {
            // Blackbox enabled and stabilization enabled and status == STAB or LAND
            // Open new file
            startNewFile();
        }
        if (fileStarted && blackboxEnabled && stabilizationEnabled && newPositionFlag) {
            // Continue pushing data
            // If there is new position, push it and uncheck the flag
            pushPosition();
            newPositionFlag = false;
        }
    }

    /**
     * Creates new file and writes headers
     */
    private void startNewFile() {
        try {
            logger.warn("Starting new file");
            // Create streams
            fileOutputStream = new FileOutputStream(FileWorkers.createBlackboxFile(blackboxDirectory));
            outputStreamWriter = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8);
            bufferedWriter = new BufferedWriter(outputStreamWriter);
            // Push info header
            pushHeader();
            // Set started flag
            fileStarted = true;
            // Mark start time
            timeStart = System.currentTimeMillis();
        } catch (Exception e) {
            logger.error("Error starting new file!", e);
        }
    }

    /**
     * Closes all streams and file
     */
    private void closeFile() {
        if (fileStarted) {
            try {
                logger.info("Closing file");
                bufferedWriter.close();
                outputStreamWriter.close();
                fileOutputStream.close();
            } catch (Exception e) {
                logger.error("Error closing file!", e);
            }
            fileStarted = false;
        }
    }

    /**
     * Writes header to the file
     */
    private void pushHeader() {
        try {
            bufferedWriter.write("time,x,y,z,yaw,setpointX,setpointY,setpointZ,setpointYaw," +
                    "ddcX,ddcY,ddcZ,ddcRoll,ddcPitch,ddcYaw,frameX,frameY,exposure,speed,status");
            bufferedWriter.write("\n");
            bufferedWriter.flush();
        } catch (IOException e) {
            logger.error("Error pushing header to the file!", e);
        }
    }

    /**
     * Writes all the data line by line with comma separator
     */
    private void pushPosition() {
        try {
            bufferedWriter.write(String.valueOf(System.currentTimeMillis() - timeStart));
            bufferedWriter.write(",");
            bufferedWriter.write(decimalFormat.format(positionContainer.x));
            bufferedWriter.write(",");
            bufferedWriter.write(decimalFormat.format(positionContainer.y));
            bufferedWriter.write(",");
            bufferedWriter.write(decimalFormat.format(positionContainer.z));
            bufferedWriter.write(",");
            bufferedWriter.write(decimalFormat.format(positionContainer.yaw));
            bufferedWriter.write(",");
            bufferedWriter.write(decimalFormat.format(positionContainer.setpointX));
            bufferedWriter.write(",");
            bufferedWriter.write(decimalFormat.format(positionContainer.setpointY));
            bufferedWriter.write(",");
            bufferedWriter.write(decimalFormat.format(positionContainer.setpointZ));
            bufferedWriter.write(",");
            bufferedWriter.write(decimalFormat.format(positionContainer.setpointYaw));
            bufferedWriter.write(",");
            bufferedWriter.write(decimalFormat.format(positionContainer.ddcX));
            bufferedWriter.write(",");
            bufferedWriter.write(decimalFormat.format(positionContainer.ddcY));
            bufferedWriter.write(",");
            bufferedWriter.write(decimalFormat.format(positionContainer.ddcZ));
            bufferedWriter.write(",");
            bufferedWriter.write(decimalFormat.format(positionContainer.ddcRoll));
            bufferedWriter.write(",");
            bufferedWriter.write(decimalFormat.format(positionContainer.ddcPitch));
            bufferedWriter.write(",");
            bufferedWriter.write(decimalFormat.format(positionContainer.ddcYaw));
            bufferedWriter.write(",");
            bufferedWriter.write(String.valueOf((int)positionContainer.frameCurrent.x));
            bufferedWriter.write(",");
            bufferedWriter.write(String.valueOf((int)positionContainer.frameCurrent.y));
            bufferedWriter.write(",");
            bufferedWriter.write(decimalFormat.format(platformContainer.cameraExposure));
            bufferedWriter.write(",");
            bufferedWriter.write(decimalFormat.format(platformContainer.speed));
            bufferedWriter.write(",");
            bufferedWriter.write(String.valueOf(positionContainer.status));
            bufferedWriter.write("\n");
            bufferedWriter.flush();
        } catch (Exception e) {
            logger.error("Error pushing logs to the file!", e);
        }
    }
}
