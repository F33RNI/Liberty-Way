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

#ifdef GPS

 /// <summary>
 /// Initializes the gps baud rate
 /// </summary>
void gps_setup(void) {
	// Open serial port
	GPS_SERIAL.begin(GPS_BAUD_RATE);
	delay(200);
}

/// <summary>
/// Reads GPS data from the serial port
/// </summary>
void gps_read(void) {
	// Signal lost watchdog counter
	if (gps_lost_counter < UINT16_MAX)
		gps_lost_counter++;

	while (GPS_SERIAL.available()) {
		// Read current byte
		gps_buffer[gps_buffer_position] = GPS_SERIAL.read();

		if (gps_byte_previous == GPS_SUFFIX_1 && gps_buffer[gps_buffer_position] == GPS_SUFFIX_2) {
			// If data suffix appears
			// Reset buffer position
			gps_buffer_position = 0;

			// Reset check sum
			gps_check_byte = 0;

			// Calculate check sum
			for (gps_temp_byte = 0; gps_temp_byte <= 7; gps_temp_byte++)
				gps_check_byte ^= gps_buffer[gps_temp_byte];

			if (gps_check_byte == gps_buffer[8]) {
				// If the check sums are equal
				// Reset watchdog
				gps_lost_counter = 0;

				// GPS position
				l_lat_gps = (int32_t)gps_buffer[3] | (int32_t)gps_buffer[2] << 8 | (int32_t)gps_buffer[1] << 16 | (int32_t)gps_buffer[0] << 24;
				l_lon_gps = (int32_t)gps_buffer[7] | (int32_t)gps_buffer[6] << 8 | (int32_t)gps_buffer[5] << 16 | (int32_t)gps_buffer[4] << 24;

				// Number of satellites
				//number_used_sats = gps_buffer[9];
				number_used_sats = 8;

				// HDOP (multiplied by 10)
				//hdop = gps_buffer[10];
				hdop = 0;

				// Altitude (multiplied by 10)
				//altitude = (int16_t)gps_buffer[12] | (int16_t)gps_buffer[11] << 8;
				altitude = 0;

				// Ground heading (multiplied by 100)
				//ground_heading = (uint16_t)gps_buffer[14] | (uint16_t)gps_buffer[13] << 8;
				ground_heading = 0;

				// Ground speed (multiplied by 10)
				//ground_speed = (uint16_t)gps_buffer[16] | (uint16_t)gps_buffer[15] << 8;
				ground_speed = 0;
			}
			else
				gps_lost_counter = UINT16_MAX;
		}
		else {
			// Store data bytes
			gps_byte_previous = gps_buffer[gps_buffer_position];
			gps_buffer_position++;

			// Reset buffer on overflow
			if (gps_buffer_position > 10)
				gps_buffer_position = 0;
		}
	}

	// When there is no GPS information available
	if (gps_lost_counter > GPS_LOST_CYCLES) {
		// Reset some variables
		l_lat_gps = 0;
		l_lon_gps = 0;
		number_used_sats = 0;
	}
}
#endif