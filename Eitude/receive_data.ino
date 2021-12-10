/*
 * Copyright (C) 2021 Fern H. (aka Pavel Neshumov), Eitude AMLS Platform controller
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

 /*
  * ATTENTION! THIS IS A BETA VERSION OF EITUDE. INSTEAD OF GPS-MIXER,
  * WE USE GETTING GPS-COORDINATES FROM THE PHONE USING THE GPS-TO-SERIAL APP
  * MORE INFO AT: https://github.com/XxOinvizioNxX/GPS-to-Serial
  *
 */

#ifdef UDP_PORT
/// <summary>
/// Callback that receives packet from UDP
/// </summary>
void udp_receive_data(uint16_t dest_port, uint8_t src_ip[IP_LEN], uint16_t src_port, const char* data, uint16_t len) {
	// Set started flag
	started = 1;

	// Parse each single byte
	for (count_var = 0; count_var < len; count_var++)
		parse_byte(data[count_var]);
}
#else
/// <summary>
/// Reads packet from serial port
/// </summary>
void serial_receive_data() {
	// Parse each single available byte
	while (COMMUNICATION_SERIAL.available())
		parse_byte(COMMUNICATION_SERIAL.read());
}
#endif

/// <summary>
/// Parses received byte to buffer
/// </summary>
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

			// Read grips command
			grips_command = rx_buffer[2];

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
