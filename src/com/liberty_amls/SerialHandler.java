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

import com.fazecast.jSerialComm.SerialPort;
import org.apache.log4j.Logger;
import java.util.ArrayList;
import java.util.List;

public class SerialHandler {
    private final Logger logger = Logger.getLogger(this.getClass().getSimpleName());
    private final String platformPortName, linkPortName;
    private int platformBaudrate, linkBaudrate;
    private SerialPort platformPort, linkPort;
    public boolean platformPortOpened;
    public boolean linkPortOpened;
    public byte[] platformData;
    public byte[] linkData;

    /**
     * Discovers available Serial ports
     * @return List of Serial ports
     */
    public static List<String> getPortNames() {
        List<String> serialPortNames = new ArrayList<>();
        SerialPort[] serialPorts = SerialPort.getCommPorts();
        for (SerialPort serialport : serialPorts) {
            String portName = serialport.getSystemPortName();
            serialPortNames.add(portName);
        }
        return serialPortNames;
    }

    /**
     * This class takes an array of bytes (linkData and platformData) and sends it over a Serial Port
     * @param platformPortName name of Platform Port (ex. 'COM1' on Windows or '/dev/ttyS0' on Linux)
     * @param platformBaudrate baudrate of Platform Port (ex. '57600')
     * @param linkPortName name of drone communication port (Liberty-Link) (ex. 'COM1' on Windows or '/dev/ttyS0' on Linux)
     * @param linkBaudrate baudrate drone communication port (Liberty-Link) (ex. '57600')
     */
    public SerialHandler(String platformPortName, String platformBaudrate, String linkPortName, String linkBaudrate) {
        this.platformPortName = platformPortName;
        this.linkPortName = linkPortName;
        if (platformPortName != null)
            this.platformBaudrate = Integer.parseInt(platformBaudrate);
        if (linkPortName != null)
            this.linkBaudrate = Integer.parseInt(linkBaudrate);
    }

    /**
     * Opens provided ports
     */
    public void openPorts() {
        platformPortOpened = false;
        linkPortOpened = false;
        if (platformPortName == null && linkPortName == null)
            // If no ports provided
            logger.warn("No serial ports presented. Nothing to open");
        else {
            try {
                if (platformPortName != null) {
                    // If Platform port provided
                    platformPort = SerialPort.getCommPort(platformPortName);
                    // Set baudrate
                    platformPort.setBaudRate(platformBaudrate);
                    // Open it
                    platformPort.openPort();
                    // Wait some time for correct opening
                    Thread.sleep(500);

                    // Check if port is open
                    if (platformPort.isOpen()) {
                        platformPortOpened = true;
                        logger.info("Platform Port opened successfully.");
                    }
                }
                if (linkPortName != null) {
                    // If drone communication port (RF) provided
                    linkPort = SerialPort.getCommPort(linkPortName);
                    // Set baudrate
                    linkPort.setBaudRate(linkBaudrate);
                    // Open it
                    linkPort.openPort();
                    // Wait some time for correct opening
                    Thread.sleep(500);

                    // Check if port is open
                    if (linkPort.isOpen()) {
                        linkPortOpened = true;
                        logger.info("Liberty-Link Port opened successfully.");
                    }
                }
            } catch (Exception e) {
                logger.error("Error opening serial ports!", e);
                // Exit because Serial Port is a vital node when turned on
                System.exit(1);
            }
        }
    }

    /**
     * Pushes byte arrays to Liberty-Link port
     */
    public void pushLinkData() {
        try {
            if (linkPortOpened && linkData != null && linkData.length > 0) {
                // RF Port
                linkPort.writeBytes(linkData, linkData.length);
                // Flush port for safe
                linkPort.getOutputStream().flush();
            }
        } catch (Exception e) {
            logger.error("Error pushing data over serial!", e);
        }
    }

    /**
     * Pushes byte arrays to Platform controller port
     */
    public void pushPlatformData() {
        try {
            if (platformPortOpened && platformData != null && platformData.length > 0) {
                // Platform Port
                platformPort.writeBytes(platformData, platformData.length);
                // Flush port for safe
                platformPort.getOutputStream().flush();
            }
        } catch (Exception e) {
            logger.error("Error pushing data over serial!", e);
        }
    }

    /**
     * Reads all available bytes from platform port
     * @return bytes array from platform port
     */
    public byte[] readDataFromPlatform() {
        int bytesAvailable = platformPort.bytesAvailable();
        byte[] buffer = new byte[bytesAvailable];
        platformPort.readBytes(buffer, bytesAvailable);
        return buffer;
    }

    /**
     * Closes ports
     */
    public void closePorts() {
        logger.info("Closing ports.");
        if (platformPortOpened) {
            platformPortOpened = false;
            try {
                platformPort.closePort();
            } catch (Exception e) {
                logger.error("Error closing Platform Port", e);
            }
        }
        if (linkPortOpened) {
            linkPortOpened = false;
            try {
                linkPort.closePort();
            } catch (Exception e) {
                logger.error("Error closing RF Port", e);
            }
        }
    }
}
