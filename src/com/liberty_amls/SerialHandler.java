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

import com.fazecast.jSerialComm.SerialPort;
import org.apache.log4j.Logger;
import java.util.ArrayList;
import java.util.List;

public class SerialHandler {
    private final Logger logger = Logger.getLogger(this.getClass().getSimpleName());
    private final String platformPortName, linkPortName;
    private final int reconnectTime;
    private final int platformBaudrate, linkBaudrate;
    private SerialPort platformPort, linkPort;
    private boolean platformPortOpened, platformPortLost = false;
    private boolean linkPortOpened, linkPortLost = false;
    private long platformPortLostTimer = 0, linkPortLostTimer = 0;
    private byte[] platformData;
    private byte[] linkData;

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
    public SerialHandler(String platformPortName, String platformBaudrate,
                         String linkPortName, String linkBaudrate, int reconnectTime) {
        this.platformPortName = platformPortName;
        this.linkPortName = linkPortName;
        this.reconnectTime = reconnectTime;
        if (platformPortName != null && platformPortName.length() > 0)
            this.platformBaudrate = Integer.parseInt(platformBaudrate);
        else
            this.platformBaudrate = 0;
        if (linkPortName != null && linkPortName.length() > 0)
            this.linkBaudrate = Integer.parseInt(linkBaudrate);
        else
            this.linkBaudrate = 0;
    }

    /**
     * Opens provided ports
     */
    public void openPorts() {
        platformPortOpened = false;
        linkPortOpened = false;
        if ((platformPortName == null || platformPortName.length() == 0) &&
                (linkPortName == null || linkPortName.length() == 0))
            // If no ports provided
            logger.warn("No serial ports presented. Nothing to open");
        else {
            try {
                openPlatformPort();
                Thread.sleep(500);
                openLinkPort();
                Thread.sleep(500);
            } catch (Exception e) {
                logger.error("Error opening serial ports!", e);
                // Exit because Serial Port is a vital node when turned on
                System.exit(1);
            }
        }
    }

    /**
     * Opens platform port if platformPortName is provided
     */
    private void openPlatformPort() {
        if (platformPortName != null && platformPortName.length() > 0) {
            // If Platform port provided
            platformPort = SerialPort.getCommPort(platformPortName);
            // Set baudrate
            platformPort.setBaudRate(platformBaudrate);
            // Open it
            platformPort.openPort();

            // Check if port is open
            if (platformPort.isOpen()) {
                platformPortOpened = true;
                platformPortLost = false;
                logger.info("Platform Port opened successfully.");
            }
        }
    }

    /**
     * Opens Liberty-Link port if platformPortName is provided
     */
    private void openLinkPort() {
        if (linkPortName != null && linkPortName.length() > 0) {
            // If drone communication port (RF) provided
            linkPort = SerialPort.getCommPort(linkPortName);
            // Set baudrate
            linkPort.setBaudRate(linkBaudrate);
            // Open it
            linkPort.openPort();

            // Check if port is open
            if (linkPort.isOpen()) {
                linkPortOpened = true;
                linkPortLost = false;
                logger.info("Liberty-Link Port opened successfully.");
            }
        }
    }

    /**
     * @return true if platform port is opened
     */
    public boolean isPlatformPortOpened() {
        return platformPortOpened;
    }

    /**
     * @return true if Liberty-Link port is opened
     */
    public boolean isLinkPortOpened() {
        return linkPortOpened;
    }

    /**
     * Fills platform data buffer
     * @param platformData bytes buffer to send
     */
    public void setPlatformData(byte[] platformData) {
        this.platformData = platformData;
    }

    /**
     * Fills Liberty-Link data buffer
     * @param linkData bytes buffer to send
     */
    public void setLinkData(byte[] linkData) {
        this.linkData = linkData;
    }

    /**
     * Pushes byte arrays to Liberty-Link port
     */
    public void pushLinkData() {
        if (!linkPortLost) {
            try {
                if (linkPortOpened && linkData != null && linkData.length > 0) {
                    // RF Port
                    linkPort.writeBytes(linkData, linkData.length);
                    // Flush port for safe
                    linkPort.getOutputStream().flush();
                }
            } catch (Exception e) {
                logger.error("Error pushing data over serial!", e);
                linkPortLost = true;
                linkPortOpened = false;
                linkPortLostTimer = System.currentTimeMillis();
            }
        } else if (System.currentTimeMillis() - linkPortLostTimer >= reconnectTime) {
            // Try to reopen serial port
            reopenLinkPort();
            linkPortLostTimer = System.currentTimeMillis();
        }
    }

    /**
     * Reads all available bytes from Liberty-Link port
     * @return bytes array from Liberty-Link port
     */
    public byte[] readDataFromLink() {
        if (!linkPortLost) {
            if (linkPortOpened) {
                try {
                    int bytesAvailable = linkPort.bytesAvailable();
                    byte[] buffer = new byte[bytesAvailable];
                    linkPort.readBytes(buffer, bytesAvailable);
                    return buffer;
                } catch (Exception e) {
                    logger.error("Error reading data from Liberty-Link port!", e);
                    linkPortLost = true;
                    linkPortOpened = false;
                    linkPortLostTimer = System.currentTimeMillis();
                }
            }
        } else if (System.currentTimeMillis() - linkPortLostTimer >= reconnectTime) {
            // Try to reopen serial port
            reopenLinkPort();
            linkPortLostTimer = System.currentTimeMillis();
        }
        return new byte[0];
    }

    /**
     * Pushes byte arrays to Platform controller port
     */
    public void pushPlatformData() {
        if (!platformPortLost) {
            // If port is not lost
            try {
                if (platformPortOpened && platformData != null && platformData.length > 0) {
                    // Platform Port
                    platformPort.writeBytes(platformData, platformData.length);
                    // Flush port for safe
                    platformPort.getOutputStream().flush();
                }
            } catch (Exception e) {
                logger.error("Error pushing data over serial!", e);
                platformPortLost = true;
                platformPortOpened = false;
                platformPortLostTimer = System.currentTimeMillis();
            }
        } else if (System.currentTimeMillis() - platformPortLostTimer >= reconnectTime) {
            // Try to reopen serial port
            reopenPlatformPort();
            platformPortLostTimer = System.currentTimeMillis();
        }
    }

    /**
     * Reads all available bytes from platform port
     * @return bytes array from platform port
     */
    public byte[] readDataFromPlatform() {
        if (!platformPortLost) {
            // If port is not lost
            if (platformPortOpened) {
                try {
                    int bytesAvailable = platformPort.bytesAvailable();
                    byte[] buffer = new byte[bytesAvailable];
                    platformPort.readBytes(buffer, bytesAvailable);
                    return buffer;
                } catch (Exception e) {
                    logger.error("Error reading data from platform port!", e);
                    platformPortLost = true;
                    platformPortOpened = false;
                    platformPortLostTimer = System.currentTimeMillis();
                }
            }
        } else if (System.currentTimeMillis() - platformPortLostTimer >= reconnectTime) {
            // Try to reopen serial port
            reopenPlatformPort();
            platformPortLostTimer = System.currentTimeMillis();
        }
        return new byte[0];
    }

    /**
     * Tries to close and reopen platform port (in case of connection lost)
     */
    private void reopenPlatformPort() {
        try {
            logger.warn("Attempt to reopen platform port");

            // Close port
            platformPort.closePort();

            // Open port
            openPlatformPort();
        } catch (Exception e) {
            logger.error("Error reopening platform port!", e);
        }
    }

    /**
     * Tries to close and reopen Liberty-Link port (in case of connection lost)
     */
    private void reopenLinkPort() {
        try {
            logger.warn("Attempt to reopen Liberty-Link port");

            // Close port
            linkPort.closePort();

            // Open port
            openLinkPort();
        } catch (Exception e) {
            logger.error("Error reopening Liberty-Link port", e);
        }
    }

    /**
     * Closes ports
     */
    public void closePorts() {
        logger.warn("Closing serial ports");
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
