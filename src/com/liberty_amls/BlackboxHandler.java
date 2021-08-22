/*
 * Copyright (C) 2021 Fern Hertz (Pavel Neshumov), Liberty-Way Landing System Project
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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BlackboxHandler implements Runnable {
    private final Logger logger = Logger.getLogger(this.getClass().getSimpleName());
    private final DecimalFormat decimalFormat = new DecimalFormat("#.#");
    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
    private final PositionContainer positionContainer;
    private final PlatformContainer platformContainer;
    private final TelemetryContainer telemetryContainer;
    private final String blackboxDirectory;
    private boolean fileStarted = false;
    private BufferedWriter bufferedWriter;
    private FileOutputStream fileOutputStream;
    private OutputStreamWriter outputStreamWriter;
    private boolean blackboxEnabled = false;
    private boolean newEntryFlag = false;
    private volatile boolean handlerRunning;

    /**
     * This class writes the current state of the drone to log files
     * TODO: Add drone telemetry
     * @param positionContainer container of the current position
     * @param blackboxDirectory Folder where .csv log files are stored
     */
    BlackboxHandler(PositionContainer positionContainer,
                    PlatformContainer platformContainer,
                    TelemetryContainer telemetryContainer,
                    String blackboxDirectory) {
        this.positionContainer = positionContainer;
        this.platformContainer = platformContainer;
        this.telemetryContainer = telemetryContainer;
        this.blackboxDirectory = blackboxDirectory;
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
     * Set to true if there is new data to log
     */
    public void newEntryFlag() {
        this.newEntryFlag = true;
    }

    /**
     * Enables or disables blackbox logs
     */
    public void setBlackboxEnabled(boolean blackboxEnabled) {
        this.blackboxEnabled = blackboxEnabled;
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
        if (fileStarted && !blackboxEnabled) {
            // Blackbox disabled
            // Close file
            closeFile();
        }
        if (!fileStarted && blackboxEnabled) {
            // Blackbox enabled and stabilization enabled
            // Open new file
            startNewFile();
        }
        if (fileStarted && blackboxEnabled && newEntryFlag) {
            // Continue pushing data
            // If there is new entry, push it and uncheck the flag
            pushPosition();
            newEntryFlag = false;
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
                    "ddcX,ddcY,ddcZ,ddcRoll,ddcPitch,ddcYaw,frameX,frameY,exposure,status," +
                    "platformLost,platformErrorStatus,platformSatellitesNum,platformLat,platformLon," +
                    "platformPressure,platformSpeed,platformHeading,platformIllumination,backlight,gripsCommand," +
                    "telemetryLost,droneErrorStatus,droneFlightMode,droneBatteryVoltage,droneSatellitesNum," +
                    "droneLat,droneLon,droneAltitude,droneSpeed,droneAngleRoll,droneAnglePitch,droneAngleYaw," +
                    "droneTemperature,droneIllumination,droneLinkWaypointStep");
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
            bufferedWriter.write(simpleDateFormat.format(new Date()));
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
            bufferedWriter.write(String.valueOf(positionContainer.status));
            bufferedWriter.write(",");
            bufferedWriter.write(String.valueOf(platformContainer.platformLost));
            bufferedWriter.write(",");
            bufferedWriter.write(String.valueOf(platformContainer.errorStatus));
            bufferedWriter.write(",");
            bufferedWriter.write(String.valueOf(platformContainer.satellitesNum));
            bufferedWriter.write(",");
            bufferedWriter.write(String.valueOf(platformContainer.gps.getLatDouble()));
            bufferedWriter.write(",");
            bufferedWriter.write(String.valueOf(platformContainer.gps.getLonDouble()));
            bufferedWriter.write(",");
            bufferedWriter.write(String.valueOf(platformContainer.pressure));
            bufferedWriter.write(",");
            bufferedWriter.write(decimalFormat.format(platformContainer.speed));
            bufferedWriter.write(",");
            bufferedWriter.write(String.valueOf((int)Math.toDegrees(platformContainer.headingRadians)));
            bufferedWriter.write(",");
            bufferedWriter.write(String.valueOf((int)platformContainer.illumination));
            bufferedWriter.write(",");
            bufferedWriter.write(String.valueOf(platformContainer.backlight));
            bufferedWriter.write(",");
            bufferedWriter.write(String.valueOf(platformContainer.gripsCommand));
            bufferedWriter.write(",");
            bufferedWriter.write(String.valueOf(telemetryContainer.telemetryLost));
            bufferedWriter.write(",");
            bufferedWriter.write(String.valueOf(telemetryContainer.errorStatus));
            bufferedWriter.write(",");
            bufferedWriter.write(String.valueOf(telemetryContainer.flightMode));
            bufferedWriter.write(",");
            bufferedWriter.write(decimalFormat.format(telemetryContainer.batteryVoltage));
            bufferedWriter.write(",");
            bufferedWriter.write(String.valueOf(telemetryContainer.satellitesNum));
            bufferedWriter.write(",");
            bufferedWriter.write(String.valueOf(telemetryContainer.gps.getLatDouble()));
            bufferedWriter.write(",");
            bufferedWriter.write(String.valueOf(telemetryContainer.gps.getLonDouble()));
            bufferedWriter.write(",");
            bufferedWriter.write(String.valueOf(telemetryContainer.altitude));
            bufferedWriter.write(",");
            bufferedWriter.write(decimalFormat.format(telemetryContainer.groundSpeed));
            bufferedWriter.write(",");
            bufferedWriter.write(String.valueOf(telemetryContainer.angleRoll));
            bufferedWriter.write(",");
            bufferedWriter.write(String.valueOf(telemetryContainer.anglePitch));
            bufferedWriter.write(",");
            bufferedWriter.write(String.valueOf(telemetryContainer.angleYaw));
            bufferedWriter.write(",");
            bufferedWriter.write(decimalFormat.format(telemetryContainer.temperature));
            bufferedWriter.write(",");
            bufferedWriter.write(decimalFormat.format(telemetryContainer.illumination));
            bufferedWriter.write(",");
            bufferedWriter.write(String.valueOf(telemetryContainer.linkWaypointStep));
            bufferedWriter.write("\n");
            bufferedWriter.flush();
        } catch (Exception e) {
            logger.error("Error pushing logs to the file!", e);
        }
    }
}
