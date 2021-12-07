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

/// <summary>
/// Fills tx_buffer and sends it back to the host
/// </summary>
void transmit_data(void) {
	// Send the error as a byte
	tx_buffer[0] = error;

#ifdef GPS
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

#else
	tx_buffer[1] = 0;
	tx_buffer[2] = 0;
	tx_buffer[3] = 0;
	tx_buffer[4] = 0;
	tx_buffer[5] = 0;
	tx_buffer[6] = 0;
	tx_buffer[7] = 0;
	tx_buffer[8] = 0;
	tx_buffer[9] = 0;
	tx_buffer[10] = 0;
	tx_buffer[11] = 0;
	tx_buffer[12] = 0;
	tx_buffer[13] = 0;
#endif

	// Send illumination variable as a byte
	tx_buffer[14] = lux_sqrt_data;

	// Calculate and send the check-byte
	tx_buffer[15] = 0;
	for (count_var = 0; count_var <= 14; count_var++)
		tx_buffer[15] ^= tx_buffer[count_var];

	// Push data to the serial or UDP port
#ifdef UDP_PORT
	//ether.sendUdp(tx_buffer, sizeof tx_buffer, UDP_PORT, destination_ip, UDP_PORT);
	ether.makeUdpReply(tx_buffer, sizeof tx_buffer, UDP_PORT);
#else
	COMMUNICATION_SERIAL.write(tx_buffer, sizeof tx_buffer);
#endif

	// Reset tx flag
	tx_flag = 0;
}
