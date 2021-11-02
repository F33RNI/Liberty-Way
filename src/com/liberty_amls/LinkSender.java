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

public class LinkSender {
    private final Logger logger = Logger.getLogger(this.getClass().getSimpleName());
    private final SerialHandler serialHandler;
    private final UDPHandler udpHandler;
    private final byte[] linkBuffer;

    /**
     * This class forms a Liberty-Link protocol packet
     * and sends it to the drone in two possible ways: via a serial or UDP port
     * @param serialHandler SerialHandler class object to send data
     * @param udpHandler UDPHandler class object to send data
     * @param settingsContainer SettingsContainer class that stores droneDataSuffix1 and droneDataSuffix2
     */
    LinkSender(SerialHandler serialHandler, UDPHandler udpHandler, SettingsContainer settingsContainer) {
        this.serialHandler = serialHandler;
        this.udpHandler = udpHandler;
        this.linkBuffer = new byte[12];
        this.linkBuffer[10] = settingsContainer.droneDataSuffix1;
        this.linkBuffer[11] = settingsContainer.droneDataSuffix2;
    }

    /**
     * Sends IDLE command to the drone (requests telemetry)
     * Link command = 0b10000000
     */
    public void sendIDLE() {
        pushCommand(0, 0);
    }

    /**
     * Sends direct corrections (from positionContainer) to the drone (optical stabilization & landing)
     * Link command = 1
     */
    public void sendDDC(int ddcRoll, int ddcPitch, int ddcZ, int ddcYaw) {
        // Form the DDC data package
        // Roll
        linkBuffer[0] = (byte) ((ddcRoll >> 8) & 0xFF);
        linkBuffer[1] = (byte) (ddcRoll & 0xFF);
        // Pitch
        linkBuffer[2] = (byte) ((ddcPitch >> 8) & 0xFF);
        linkBuffer[3] = (byte) (ddcPitch & 0xFF);
        // Yaw
        linkBuffer[4] = (byte) ((ddcYaw >> 8) & 0xFF);
        linkBuffer[5] = (byte) (ddcYaw & 0xFF);
        // Throttle
        linkBuffer[6] = (byte) ((ddcZ >> 8) & 0xFF);
        linkBuffer[7] = (byte) (ddcZ & 0xFF);

        // Direct control mode (P = 1, CCC = 001, XXXX = 0000)
        linkBuffer[8] = (byte) 0b10010000;

        // Transmit direct control data
        pushLinkData();
    }

    /**
     * Sends gps waypoint
     * Link command = 3
     * @param gps GPS position
     */
    public void sendGPSWaypoint(GPS gps) {
        // Get integer values
        int latInt = gps.getLatInt();
        int lonInt = gps.getLonInt();

        // Lat
        linkBuffer[0] = (byte) ((latInt >> 24) & 0xFF);
        linkBuffer[1] = (byte) ((latInt >> 16) & 0xFF);
        linkBuffer[2] = (byte) ((latInt >> 8) & 0xFF);
        linkBuffer[3] = (byte) (latInt & 0xFF);

        // Lon
        linkBuffer[4] = (byte) ((lonInt >> 24) & 0xFF);
        linkBuffer[5] = (byte) ((lonInt >> 16) & 0xFF);
        linkBuffer[6] = (byte) ((lonInt >> 8) & 0xFF);
        linkBuffer[7] = (byte) (lonInt & 0xFF);

        // Waypoint fill mode (P = 0, CCC = 011, XXXX = 0000)
        linkBuffer[8] = (byte) 0b00110000;

        // Transmit GPS coordinates
        pushLinkData();
    }

    /**
     * Sends a command to turn off the motors.
     * Link command = 0b11100000
     */
    public void sendMotorsOFF() {
        pushCommand(0b110, 0);
    }

    /**
     * Sends a command to start auto-takeoff sequence
     * Link command = 0b10100000
     */
    public void sendTakeoff() {
        pushCommand(0b010, 0);
    }

    /**
     * Sends a command to start auto-landing sequence
     * Link command = 0b10100000
     */
    public void sendLand() {
        pushCommand(0b010, 0);
    }


    /**
     * Sends a command to execute flight termination system
     * Link command = 0b11111111
     */
    public void sendFTS() {
        logger.error("Sending FTS command!");
        pushCommand(0b111, 0b1111);
    }

    /**
     * Pushes command (P = 1) (PCCCXXXX)
     * @param commandBits first 3 bits of command (after P bit) - CCC
     * @param bodyBits last 4 bits of command (aka data bits) - XXXX
     */
    private void pushCommand(int commandBits, int bodyBits) {
        // Reset body bytes
        for (int i = 0; i <= 7; i++)
            linkBuffer[i] = 0;

        // Parse command
        linkBuffer[8] = (byte) (((commandBits << 4) | 0b10000000 | bodyBits) & 0xFF);

        // Transmit data
        pushLinkData();
    }

    /**
     * Pushes bytes buffer to Liberty-Link port via Serial or UDP
     */
    private void pushLinkData() {
        // Calculate check byte
        byte checkByte = 0;
        for (int i = 0; i <= 8; i++)
            checkByte = (byte) (checkByte ^ linkBuffer[i]);
        linkBuffer[9] = checkByte;

        // Transmit data
        serialHandler.setDataBuffer(linkBuffer);
        udpHandler.setUdpData(linkBuffer);
        serialHandler.pushData();
        udpHandler.pushData();
    }
}
