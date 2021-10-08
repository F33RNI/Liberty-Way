/*
 * Copyright (C) 2021 Fern H. (aka Pavel Neshumov), Liberty-Way Landing System Project
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

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class UDPHandler implements Runnable {
    private final Logger logger = Logger.getLogger(this.getClass().getSimpleName());
    private DatagramSocket datagramSocket;
    private final String udpIPPort;
    private final int udpTimeout;
    private InetAddress inetAddress;
    private int port;
    private boolean udpPortOpened = false;
    private byte[] udpData;
    private final byte[] packetBuffer = new byte[1024];
    private final BlockingQueue<Byte> receiveBuffer = new ArrayBlockingQueue<>(1024);
    private volatile boolean handlerRunning = false;

    /**
     * This class takes an array of bytes (udpData) and sends it as a packet via an UDP
     * @param udpIPPort String with format 'IP:PORT'
     */
    public UDPHandler(String udpIPPort, int udpTimeout) {
        this.udpIPPort = udpIPPort;
        this.udpTimeout = udpTimeout;
    }

    /**
     * Parses udpIPPort into inetAddress and port and creates the datagramSocket object
     */
    public void openUDP() {
        try {
            if (udpIPPort != null && udpIPPort.length() > 0) {
                inetAddress = InetAddress.getByName(udpIPPort.split(":")[0]);
                port = Integer.parseInt(udpIPPort.split(":")[1]);
                datagramSocket = new DatagramSocket(port);
                udpData = new byte[1];
                udpPortOpened = true;
                pushData();
            }
        } catch (Exception e) {
            udpPortOpened = false;
            logger.error("Error starting UDP socket " + udpIPPort, e);
            // Exit because UDP is a vital node when turned on
            System.exit(1);
        }
    }

    @Override
    public void run() {
        if (udpPortOpened)
            handlerRunning = true;
        while (handlerRunning)
            updReader();
    }

    /**
     * Pushes data as a datagramPacket via an UDP
     */
    public void pushData() {
        try {
            if (udpPortOpened && udpData != null && udpData.length > 0) {
                DatagramPacket packet = new DatagramPacket(
                        udpData, udpData.length, inetAddress, port
                );
                datagramSocket.send(packet);
            }
        } catch (Exception e) {
            logger.error("Error pushing data to " + udpIPPort, e);
        }
    }

    /**
     * @return size of buffer. If size > 0, it means that new data has arrived
     */
    public int getBufferSize() {
        return receiveBuffer.size();
    }

    /**
     * Blocks until at least 1 byte of data is received into the buffer
     * @return byte from UDP port
     */
    public byte takeSingleByte() {
        try {
            return receiveBuffer.take();
        } catch (Exception e) {
            logger.error("Error taking byte from buffer!", e);
        }
        return 0;
    }

    /**
     * Reads all available bytes from UDP port
     */
    private void updReader() {
        try {
            if (udpPortOpened) {
                // New receiving packet
                DatagramPacket packet = new DatagramPacket(packetBuffer, packetBuffer.length);

                // Block until a packet is received
                datagramSocket.setSoTimeout(udpTimeout);
                datagramSocket.receive(packet);

                // Add bytes to the buffer
                for (int i = 0; i < packet.getLength(); i++) {
                    receiveBuffer.add(packetBuffer[i + packet.getOffset()]);
                }
            }
        } catch (SocketTimeoutException e) {
            logger.error("Timeout reading data from " + udpIPPort);
        } catch (Exception e) {
            logger.error("Error reading data from " + udpIPPort, e);
        }
    }

    /**
     * Sets bytes buffer to be sent to the UDP socket
     * @param udpData bytes buffer
     */
    public void setUdpData(byte[] udpData) {
        this.udpData = udpData;
    }

    /**
     * @return true if UDP port is opened
     */
    public boolean isUdpPortOpened() {
        return udpPortOpened;
    }

    /**
     * Closes datagramSocket and sets udpPortOpened flag to false
     */
    public void closeUDP() {
        logger.warn("Closing UDP port " + udpIPPort);
        handlerRunning = false;
        if (udpPortOpened)
            datagramSocket.close();
        udpPortOpened = false;
    }
}