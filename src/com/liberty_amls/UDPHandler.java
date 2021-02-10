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
import java.net.*;

public class UDPHandler {
    private final Logger logger = Logger.getLogger(this.getClass().getSimpleName());
    private DatagramSocket datagramSocket;
    private final String udpIPPort;
    private InetAddress inetAddress;
    private int port;
    public boolean udpPortOpened = false;
    public byte[] udpData;

    /**
     * This class takes an array of bytes (udpData) and sends it as a packet via an UDP
     * @param udpIPPort String with format 'IP:PORT'
     */
    public UDPHandler(String udpIPPort) {
        this.udpIPPort = udpIPPort;
    }

    /**
     * Parses udpIPPort into inetAddress and port and creates the datagramSocket object
     */
    public void openUDP() {
        try {
            if (udpIPPort != null) {
                inetAddress = InetAddress.getByName(udpIPPort.split(":")[0]);
                port = Integer.parseInt(udpIPPort.split(":")[1]);
                datagramSocket = new DatagramSocket();
                udpPortOpened = true;
            }
        } catch (Exception e) {
            logger.error("Error starting UDP socket!", e);
            // Exit because UDP is a vital node when turned on
            System.exit(1);
        }
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
            logger.error("Error pushing data over UDP!", e);
        }
    }

    /**
     * Closes datagramSocket and sets udpPortOpened flag to false
     */
    public void closeUDP() {
        if (udpPortOpened)
            datagramSocket.close();
        udpPortOpened = false;
    }
}