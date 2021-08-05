/*
 * Copyright (C) 2021 Fern Hertz (Pavel Neshumov), Liberty-Way Landing System Project
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
     * Sends IDLE command to the drone (disables all corrections and requests telemetry)
     * Link command = 0
     */
    public void sendIDLE() {
        // Reset body bytes
        for (int i = 0; i <= 7; i++)
            linkBuffer[i] = 0;

        // Link command
        linkBuffer[8] = (byte) 0;

        // Transmit data
        pushLinkData();
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
        // Link command (1 - direct control)
        linkBuffer[8] = (byte) 1;

        // Transmit direct control data
        pushLinkData();
    }

    /**
     * Sends pressure waypoint to the drone
     * Link command = 2
     * @param pressure atm. pressure in Pascals
     */
    public void sendPressureWaypoint(int pressure) {
        // Pressure waypoint
        linkBuffer[0] = (byte) ((pressure >> 24) & 0xFF);
        linkBuffer[1] = (byte) ((pressure >> 16) & 0xFF);
        linkBuffer[2] = (byte) ((pressure >> 8) & 0xFF);
        linkBuffer[3] = (byte) (pressure & 0xFF);

        // Empty other part of the packet
        linkBuffer[4] = 0;
        linkBuffer[5] = 0;
        linkBuffer[6] = 0;
        linkBuffer[7] = 0;

        // Link command
        linkBuffer[8] = (byte) 2;

        // Transmit pressure waypoint
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

        // Link command
        linkBuffer[8] = (byte) 3;

        // Transmit GPS coordinates
        pushLinkData();
    }

    /**
     * Sends a command to turn off the motors.
     * Link command = 4
     */
    public void sendMotorsStop() {
        // Reset body bytes
        for (int i = 0; i <= 7; i++)
            linkBuffer[i] = 0;

        // Link command
        linkBuffer[8] = (byte) 4;

        // Transmit data
        pushLinkData();
    }

    /**
     * Sends a command to turn off the motors.
     * Link command = 5
     */
    public void sendStartSequence() {
        logger.warn("Sending takeoff command");
        // Reset body bytes
        for (int i = 0; i <= 7; i++)
            linkBuffer[i] = 0;

        // Link command
        linkBuffer[8] = (byte) 5;

        // Transmit data
        pushLinkData();
    }

    /**
     * Sends abort command to the drone
     * (Clears flags, resets direct corrections, waypoint flags and sharply jumps up to prevent a collision)
     * Link command = 6
     */
    public void sendAbort() {
        logger.warn("Sending abort command");
        // Reset body bytes
        for (int i = 0; i <= 7; i++)
            linkBuffer[i] = 0;

        // Link command
        linkBuffer[8] = (byte) 6;

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
