/*
 * Copyright (C) 2021 Fern H. (Pavel Neshumov), Liberty-Way Landing System Project
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

public class PlatformHandler implements Runnable {
    private final Logger logger = Logger.getLogger(this.getClass().getSimpleName());
    private final SettingsContainer settingsContainer;
    private final PlatformContainer platformContainer;
    private final PositionContainer positionContainer;
    private final SerialHandler serialHandler;
    private final UDPHandler udpHandler;
    private final byte[] platformRxBuffer = new byte[22];
    private final byte[] platformTxBuffer = new byte[6];
    private byte platformRxBytePrevious = 0;
    private int platformRxBufferPosition = 0;
    private long platformLastPacketTime = 0, loopTimer = 0;
    private volatile boolean handleRunning;

    /**
     * This class communicates with the platform via the serial port
     */
    PlatformHandler(PlatformContainer platformContainer,
                    PositionContainer positionContainer,
                    SerialHandler serialHandler,
                    UDPHandler udpHandler,
                    SettingsContainer settingsContainer) {
        this.platformContainer = platformContainer;
        this.positionContainer = positionContainer;
        this.serialHandler = serialHandler;
        this.udpHandler = udpHandler;
        this.settingsContainer = settingsContainer;
        this.platformTxBuffer[4] = settingsContainer.platformDataSuffix1;
        this.platformTxBuffer[5] = settingsContainer.platformDataSuffix2;
    }

    /**
     * Starts the main loop
     */
    @Override
    public void run() {
        // Set loop flag and start main loop
        logger.info("Starting main loop");
        handleRunning = true;
        while (handleRunning)
            platformLoop();
    }

    /**
     * Main platform loop
     */
    private void platformLoop() {
        // Check lost status
        if (!platformContainer.platformLost &&
                System.currentTimeMillis() - platformLastPacketTime >= settingsContainer.platformLostTime) {
            logger.warn("Platform communication lost!");
            platformContainer.platformLost = true;
        }

        // Send data and request new data
        if (System.currentTimeMillis() - loopTimer >= settingsContainer.platformLoopTimer ||
                platformContainer.platformLost) {
            // Send request
            sendRequest();

            // Reset timer
            loopTimer = System.currentTimeMillis();
        }

        // Read and parse data from serial or UDP port
        byte[] tempBuffer = serialHandler.readData();
        if (tempBuffer != null && tempBuffer.length > 0) {
            for (byte tempByte : tempBuffer)
                readAndParse(tempByte);
        } else if (udpHandler.isUdpPortOpened() && udpHandler.getBufferSize() > 0) {
            readAndParse(udpHandler.takeSingleByte());
        }
    }

    /**
     * Reads single byte to the buffer
     * @param data single byte of data
     */
    private void readAndParse(byte data) {
        platformRxBuffer[platformRxBufferPosition] = data;
        if (platformRxBytePrevious == settingsContainer.platformDataSuffix1
                && platformRxBuffer[platformRxBufferPosition] == settingsContainer.platformDataSuffix2) {
            // If data suffix appears
            // Reset buffer position
            platformRxBufferPosition = 0;

            // Reset check sum
            byte checkByte = 0;

            // Calculate check sum
            for (int i = 0; i <= 18; i++)
                checkByte ^= platformRxBuffer[i];

            if (checkByte == platformRxBuffer[19]) {
                // Parse data if the checksums are equal

                // Error status
                platformContainer.errorStatus = ((int) platformRxBuffer[0] & 0xFF);

                // New GPS coordinates
                platformContainer.gps.setFromInt(((int) platformRxBuffer[4] & 0xFF)
                                | ((int) platformRxBuffer[3] & 0xFF) << 8
                                | ((int) platformRxBuffer[2] & 0xFF) << 16
                                | ((int) platformRxBuffer[1] & 0xFF) << 24,
                        ((int) platformRxBuffer[8] & 0xFF)
                                | ((int) platformRxBuffer[7] & 0xFF) << 8
                                | ((int) platformRxBuffer[6] & 0xFF) << 16
                                | ((int) platformRxBuffer[5] & 0xFF) << 24);

                // Number of GPS satellites
                platformContainer.gps.setSatellitesNum((int) platformRxBuffer[9] & 0xFF);

                // Ground heading
                platformContainer.gps.setGroundHeading((((int) platformRxBuffer[11] & 0xFF)
                        | ((int) platformRxBuffer[10] & 0xFF) << 8) / 10.0);

                // Platform's speed
                platformContainer.gps.setGroundSpeed((((int) platformRxBuffer[13] & 0xFF)
                        | ((int) platformRxBuffer[12] & 0xFF) << 8) / 10.0);

                // Pressure
                platformContainer.pressure = ((int) platformRxBuffer[17] & 0xFF)
                        | ((int) platformRxBuffer[16] & 0xFF) << 8
                        | ((int) platformRxBuffer[15] & 0xFF) << 16
                        | ((int) platformRxBuffer[14] & 0xFF) << 24;

                // Illumination from LUX meter
                platformContainer.illumination = Math.pow(((int) platformRxBuffer[18] & 0xFF), 2.105);

                // Increment packets counter
                platformContainer.packetsNumber++;

                // Reset timer and lost flag
                if (platformContainer.platformLost)
                    logger.warn("Platform communication restored");
                platformContainer.platformLost = false;
                platformLastPacketTime = System.currentTimeMillis();
            } else
                logger.warn("Wrong platform checksum!");

        } else {
            // Store data bytes
            platformRxBytePrevious = platformRxBuffer[platformRxBufferPosition];
            platformRxBufferPosition++;

            // Reset buffer on overflow
            if (platformRxBufferPosition > 21)
                platformRxBufferPosition = 0;
        }
    }

    /**
     * Sends system status, backlight state and grips command to the platforms
     * and requests platform data
     */
    private void sendRequest() {
        // Fill buffer
        // System status
        platformTxBuffer[0] = (byte) (positionContainer.status & 0xFF);

        // Backlight state
        platformTxBuffer[1] = (byte) (platformContainer.backlight ? 1 : 0);

        // Grips command
        platformTxBuffer[2] = (byte) (platformContainer.gripsCommand & 0xFF);

        // Check byte
        byte checkByte = 0;
        for (int i = 0; i <= 2; i++)
            checkByte = (byte) (checkByte ^ platformTxBuffer[i]);
        platformTxBuffer[3] = checkByte;

        // Transmit data
        serialHandler.setDataBuffer(platformTxBuffer);
        udpHandler.setUdpData(platformTxBuffer);
        serialHandler.pushData();
        udpHandler.pushData();
    }

    /**
     * Stops the main loop and disables platform lights and grips
     */
    public void stop() {
        // Print warning
        logger.warn("Turning off platform communication");

        // Stop main loop
        handleRunning = false;

        // Turn off backlight
        platformContainer.backlight = false;

        // Turn off grips
        platformContainer.gripsCommand = 0;

        // Send request
        sendRequest();
    }
}
