/**
 * Copyright (C) 2022 Vladislav Yasnetsky, Eitude AMLS Platform controller
 * This software is part of Liberty Drones Project
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

#include "datatypes.h"
#include "config.h"
#include "communication.h"

/**
 * @brief Initializes communication with Liberty-Way
 * 
 */
void communication_setup(void) {
	// Init ENC28J60 module
	if (ether.begin(sizeof Ethernet::buffer, MAC, SS) == 0) {
		// Failed to access Ethernet controller
		error = ERROR_ETHERNET_CONTROLLER;
		while (error != 0)
			// Show current error
			status_led();
	}

	// Set DHCP address
	//ether.dhcpSetup();
	// Set static address
	ether.staticSetup(STATIC_IP);

	if (ether.myip[0] != STATIC_IP[0] || ether.myip[1] != STATIC_IP[1] 
		|| ether.myip[2] != STATIC_IP[2] || ether.myip[3] != STATIC_IP[3]) {
		// Failed to setup static IP
		error = ERROR_IP;
		while (error != 0)
			// Show current error
			status_led();
	}

	// Start listening on udp_port
	ether.udpServerListenOnPort(&udp_receive_data, UDP_PORT);
}

/**
 * @brief Runs packetLoop
 * 
 */
void communication_ether_packet_loop(void) {
	ether.packetLoop(ether.packetReceive());
}

/**
 * @brief Receives UDP packet
 * 
 * @param dest_port 
 * @param src_ip 
 * @param src_port 
 * @param data 
 * @param len 
 */
void udp_receive_data(uint16_t dest_port, uint8_t src_ip[IP_LEN], uint16_t src_port, const char* data, uint16_t len) {
	// Set started flag
	started = 1;

	// Parse each single byte
	for (uint16_t i = 0; i < len; i++)
		parse_byte(data[i]);
}

/**
 * @brief Parses received byte to buffer
 * 
 * @param incoming_data byte to parse
 */
void parse_byte(uint8_t incoming_data) {
	// Read current byte
	rx_buffer[rx_buffer_position] = incoming_data;

	if (rx_byte_previous == PACKET_SUFFIX_1 && rx_buffer[rx_buffer_position] == PACKET_SUFFIX_2) {
		// If data suffix appears
		// Reset buffer position
		rx_buffer_position = 0;

		// Reset check sum
		check_byte = 0;

		// Calculate check sum
		for (temp_byte = 0; temp_byte <= 2; temp_byte++)
			check_byte ^= rx_buffer[temp_byte];

		if (check_byte == rx_buffer[3]) {
			// If the check sums are equal
			// Read status from Liberty-Way
			system_status = rx_buffer[0];

			// Read backlight state
			backlight_state = rx_buffer[1];

			// Read alignment command
			alignment_state = rx_buffer[2];

			// Send answer back to the host
			tx_flag = 1;
		}
	}
	else {
		// Store data bytes
		rx_byte_previous = rx_buffer[rx_buffer_position];
		rx_buffer_position++;

		// Reset buffer on overflow
		if (rx_buffer_position > 5)rx_buffer_position = 0;
	}
}

/**
 * @brief Fills tx_buffer and sends it back to the host
 * 
 */
void data_send(void) {
	// Check TX flag
	if (tx_flag) {
		// Send the error as a byte
		tx_buffer[0] = error;

		// Send bytes of the latitude position variable
		tx_buffer[1] = l_lat_gps >> 24;
		tx_buffer[2] = l_lat_gps >> 16;
		tx_buffer[3] = l_lat_gps >> 8;
		tx_buffer[4] = l_lat_gps;

		// Send bytes of the longitude position variable
		tx_buffer[5] = l_lon_gps >> 24;
		tx_buffer[6] = l_lon_gps >> 16;
		tx_buffer[7] = l_lon_gps >> 8;
		tx_buffer[8] = l_lon_gps;

		// Send the number_used_sats variable as a byte
		tx_buffer[9] = number_used_sats;

		// Send bytes of the ground_heading variable (multiplied by 100)
		tx_buffer[10] = ground_heading >> 8;
		tx_buffer[11] = ground_heading;

		// Send bytes of the ground_speed variable (multiplied by 10)
		tx_buffer[12] = ground_speed >> 8;
		tx_buffer[13] = ground_speed;

		// Send illumination variable as a byte
		tx_buffer[14] = lux_sqrt_data;

		// Calculate and send the check-byte
		tx_buffer[15] = 0;
		for (uint8_t i = 0; i <= 14; i++)
			tx_buffer[15] ^= tx_buffer[i];

		// Packet ending
		tx_buffer[16] = PACKET_SUFFIX_1;
		tx_buffer[17] = PACKET_SUFFIX_2;

		// Push data to the UDP
		//ether.sendUdp(tx_buffer, sizeof tx_buffer, UDP_PORT, destination_ip, UDP_PORT);
		ether.makeUdpReply(tx_buffer, sizeof tx_buffer, UDP_PORT);

		// Reset tx flag
		tx_flag = 0;
	}
}