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
    private final String cncPortName, rfPortName;
    private int cncBaudrate, rfBaudrate;
    private SerialPort cncPort, rfPort;
    public boolean cncPortOpened;
    public boolean rfPortOpened;
    public byte[] cncData;
    public byte[] rfData;

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
     * This class takes an array of bytes (rfData and cncData) and sends it over a Serial Port
     * @param cncPortName name of CNC Port (ex. 'COM1' on Windows or '/dev/ttyS0' on Linux)
     * @param cncBaudrate baudrate of CNC Port (ex. '57600')
     * @param rfPortName name of drone communication port port (ex. 'COM1' on Windows or '/dev/ttyS0' on Linux)
     * @param rfBaudrate baudrate drone communication port port (ex. '57600')
     */
    public SerialHandler(String cncPortName, String cncBaudrate, String rfPortName, String rfBaudrate) {
        this.cncPortName = cncPortName;
        this.rfPortName = rfPortName;
        if (cncPortName != null)
            this.cncBaudrate = Integer.parseInt(cncBaudrate);
        if (rfPortName != null)
            this.rfBaudrate = Integer.parseInt(rfBaudrate);
    }

    /**
     * Opens provided ports
     */
    public void openPorts() {
        cncPortOpened = false;
        rfPortOpened = false;
        if (cncPortName == null && rfPortName == null)
            // If no ports provided
            logger.warn("No serial ports presented. Nothing to open");
        else {
            try {
                if (cncPortName != null) {
                    // If CNC port provided
                    cncPort = SerialPort.getCommPort(cncPortName);
                    // Set baudrate
                    cncPort.setBaudRate(cncBaudrate);
                    // Open it
                    cncPort.openPort();

                    // Check if port is open
                    if (cncPort.isOpen()) {
                        cncPortOpened = true;
                        logger.info("CNC Port opened successfully.");
                    }
                }
                if (rfPortName != null) {
                    // If drone communication port (RF) provided
                    rfPort = SerialPort.getCommPort(rfPortName);
                    // Set baudrate
                    rfPort.setBaudRate(rfBaudrate);
                    // Open it
                    rfPort.openPort();

                    // Check if port is open
                    if (rfPort.isOpen()) {
                        rfPortOpened = true;
                        logger.info("RF Port opened successfully.");
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
     * Pushes byte arrays to serial ports
     */
    public void pushData() {
        try {
            if (cncPortOpened && cncData != null && cncData.length > 0) {
                // CNC Port
                cncPort.writeBytes(cncData, cncData.length);
                // Flush port for safe
                cncPort.getOutputStream().flush();
            }
            if (rfPortOpened && rfData != null && rfData.length > 0) {
                // RF Port
                rfPort.writeBytes(rfData, rfData.length);
                // Flush port for safe
                rfPort.getOutputStream().flush();
            }
        } catch (Exception e) {
            logger.error("Error pushing data over serial!", e);
        }
    }

    /**
     * Closes ports
     */
    public void closePorts() {
        logger.info("Closing ports.");
        if (cncPortOpened) {
            cncPortOpened = false;
            try {
                cncPort.closePort();
            } catch (Exception e) {
                logger.error("Error closing CNC Port", e);
            }
        }
        if (rfPortOpened) {
            rfPortOpened = false;
            try {
                rfPort.closePort();
            } catch (Exception e) {
                logger.error("Error closing RF Port", e);
            }
        }
    }
}
