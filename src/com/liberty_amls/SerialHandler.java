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

import com.fazecast.jSerialComm.SerialPort;
import org.apache.log4j.Logger;
import java.util.ArrayList;
import java.util.List;

public class SerialHandler {
    private final Logger logger = Logger.getLogger(this.getClass().getSimpleName());
    private final String portName;
    private final int baudRate;
    private final int reconnectTime;
    private SerialPort serialPort;
    private boolean portOpened, portLost = false;
    private long portLostTimer = 0;
    private byte[] dataBuffer;

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
     * This class takes an array of bytes (dataBuffer) and sends it over a Serial Port
     * @param portName name of Serial Port (ex. 'COM1' on Windows or '/dev/ttyS0' on Linux)
     * @param portBaudRate baudrate of Serial Port (ex. '57600')
     * @param reconnectTime reconnection time (in case of port lost)
     */
    public SerialHandler(String portName, String portBaudRate, int reconnectTime) {
        this.portName = portName;
        this.reconnectTime = reconnectTime;
        if (portName != null && portName.length() > 0)
            this.baudRate = Integer.parseInt(portBaudRate);
        else
            this.baudRate = 0;
    }

    /**
     * Opens provided port
     */
    public void openPort() {
        portOpened = false;
        if (portName == null || portName.length() == 0)
            // If no port provided
            logger.warn("No serial port provided. Nothing to open");
        else {
            try {
                // Initialize serial port
                serialPort = SerialPort.getCommPort(portName);

                // Set baudrate
                serialPort.setBaudRate(baudRate);

                // Open it
                serialPort.openPort();

                // Check if port is open
                if (serialPort.isOpen()) {
                    portOpened = true;
                    portLost = false;
                    logger.info("Port " + portName + " opened successfully.");
                }

                // Wait some time to complete action
                Thread.sleep(500);
            } catch (Exception e) {
                logger.error("Error opening port " + portName + "!", e);
                // Exit because Serial Port is a vital node when turned on
                System.exit(1);
            }
        }
    }

    /**
     * @return true if serial port is opened
     */
    public boolean isPortOpened() {
        return portOpened;
    }

    /**
     * Fills port data buffer
     * @param dataBuffer bytes buffer to send
     */
    public void setDataBuffer(byte[] dataBuffer) {
        this.dataBuffer = dataBuffer;
    }

    /**
     * Pushes byte arrays to the serial port
     */
    public void pushData() {
        if (!portLost) {
            try {
                if (portOpened && dataBuffer != null && dataBuffer.length > 0) {
                    // Write bytes to the port
                    serialPort.writeBytes(dataBuffer, dataBuffer.length);
                    // Flush port for safe
                    serialPort.getOutputStream().flush();
                }
            } catch (Exception e) {
                logger.error("Error pushing data over serial!", e);
                portLost = true;
                portOpened = false;
                portLostTimer = System.currentTimeMillis();
            }
        } else if (System.currentTimeMillis() - portLostTimer >= reconnectTime) {
            // Try to reopen serial port
            reopenPort();
            portLostTimer = System.currentTimeMillis();
        }
    }

    /**
     * Reads all available bytes from serial port
     * @return bytes array from serial port
     */
    public byte[] readData() {
        if (!portLost) {
            if (portOpened) {
                try {
                    int bytesAvailable = serialPort.bytesAvailable();
                    byte[] buffer = new byte[bytesAvailable];
                    serialPort.readBytes(buffer, bytesAvailable);
                    return buffer;
                } catch (Exception e) {
                    logger.error("Error reading data from " + portName + " port!", e);
                    portLost = true;
                    portOpened = false;
                    portLostTimer = System.currentTimeMillis();
                }
            }
        } else if (System.currentTimeMillis() - portLostTimer >= reconnectTime) {
            // Try to reopen serial port
            reopenPort();
            portLostTimer = System.currentTimeMillis();
        }
        return new byte[0];
    }

    /**
     * Tries to close and reopen port (in case of connection lost)
     */
    private void reopenPort() {
        try {
            logger.warn("Attempt to reopen " + portName + " port");

            // Close port
            serialPort.closePort();

            // Open port
            openPort();
        } catch (Exception e) {
            logger.error("Error reopening " + portName + " port!", e);
        }
    }

    /**
     * Closes ports
     */
    public void closePort() {
        logger.warn("Closing " + portName + " port");
        if (portOpened) {
            portOpened = false;
            try {
                serialPort.closePort();
            } catch (Exception e) {
                logger.error("Error closing " + portName + " port", e);
            }
        }
    }
}
