/*
 * Copyright (C) 2021 Fern Hertz (Pavel Neshumov), Eitude AMLS Platform controller
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

#ifdef GPS

 /// <summary>
 /// Initializes the compass on 57.6kbps baud rate
 /// </summary>
void gps_setup(void) {
	// Open serial port at default baud rate
	GPS_SERIAL.begin(9600);
	delay(250);

	// Disable GPGSV messages by using the ublox protocol
	GPS_SERIAL.write(GPS_DISABLE_GPGSV, 11);
	delay(350);

	// Set the refresh rate to 5Hz by using the ublox protocol
	GPS_SERIAL.write(GPS_SET_TO_5HZ, 14);
	delay(350);

	// Set the baud rate to 57.6kbps by using the ublox protocol
	GPS_SERIAL.write(GPS_SET_TO_57KBPS, 28);
	delay(200);

	// Open serial port at 57.6kbps
	GPS_SERIAL.begin(57600);
	delay(200);
}

/// <summary>
/// Reads raw UBLOX data from the serial port
/// </summary>
void gps_read(void) {
	// Set lost flag
	if (gps_reset_flag)
		gps_lost_counter = UINT16_MAX;

	// Signal lost watchdog counter
	if (gps_lost_counter < UINT16_MAX)
		gps_lost_counter++;

	// GPS lost flag
	if (gps_lost_counter > GPS_LOST_CYCLES) {
		l_lat_gps = 0;
		l_lon_gps = 0;
		number_used_sats = 0;
	}

	if (gps_reset_flag) {
		// Reset GPS connection
		while (GPS_SERIAL.available()) {
			if (GPS_SERIAL.read() == '*')
				gps_reset_flag = 0;
		}
	}
	else {
		// Read data from the GPS module
		while (GPS_SERIAL.available() && new_line_found == 0) {
			// Stay in this loop as long as there is serial information from the GPS available
			read_serial_byte = GPS_SERIAL.read();
			if (read_serial_byte == '$') {
				// Clear the old data from the incoming buffer array if the new byte equals a $ character
				for (message_counter = 0; message_counter <= 99; message_counter++) {
					incoming_message[message_counter] = '-';
				}
				// Reset the message_counter variable because we want to start writing at the begin of the array
				message_counter = 0;
			}
			// If the received byte does not equal a $ character, increase the message_counter variable
			else if (message_counter <= 99)
				message_counter++;

			// Write the new received byte to the new position in the incoming_message array
			incoming_message[message_counter] = read_serial_byte;

			// Every NMEA line ends with a '*'. If this character is detected the new_line_found variable is set to 1
			if (read_serial_byte == '*')
				new_line_found = 1;
		}
	}
}

/// <summary>
/// Parses GPS UBLOX protocol, executes PID controller and changes setpoints using the sticks
/// </summary>
void gps_handler(void) {
	// If the software has detected a new NMEA line it will check if it's a valid line that can be used
	if (new_line_found == 1) {
		// Reset the new_line_found variable for the next line
		new_line_found = 0;

		if (incoming_message[4] == 'L' && incoming_message[5] == 'L' && incoming_message[7] == ',') {
			// When there is no GPS fix or latitude/longitude information available
			// Turn off builtin LED
			digitalWrite(LED_BUILTIN, 0);

			// Set some variables to 0 if no valid information is found by the GPS module. This is needed for the GPS loss when flying
			l_lat_gps = 0;
			l_lon_gps = 0;
			number_used_sats = 0;
			gps_lost_counter = UINT16_MAX;

		}
		// If the line starts with GA and if there is a GPS fix we can scan the line for the latitude, longitude and number of satellites
		if (incoming_message[4] == 'G' && incoming_message[5] == 'A' && (incoming_message[44] == '1' || incoming_message[44] == '2')) {

			// Filter the minutes for the GGA line multiplied by 10
			l_lat_gps = ((int)incoming_message[19] - 48) * (long)10000000;
			l_lat_gps += ((int)incoming_message[20] - 48) * (long)1000000;
			l_lat_gps += ((int)incoming_message[22] - 48) * (long)100000;
			l_lat_gps += ((int)incoming_message[23] - 48) * (long)10000;
			l_lat_gps += ((int)incoming_message[24] - 48) * (long)1000;
			l_lat_gps += ((int)incoming_message[25] - 48) * (long)100;
			l_lat_gps += ((int)incoming_message[26] - 48) * (long)10;
			// To convert minutes to degrees we need to divide minutes by 6
			l_lat_gps /= (long)6;
			// Add multiply degrees by 10
			l_lat_gps += ((int)incoming_message[17] - 48) * (long)100000000;
			l_lat_gps += ((int)incoming_message[18] - 48) * (long)10000000;
			// Divide everything by 10
			l_lat_gps /= 10;

			// Filter minutes for the GGA line multiplied by 10
			l_lon_gps = ((int)incoming_message[33] - 48) * (long)10000000;
			l_lon_gps += ((int)incoming_message[34] - 48) * (long)1000000;
			l_lon_gps += ((int)incoming_message[36] - 48) * (long)100000;
			l_lon_gps += ((int)incoming_message[37] - 48) * (long)10000;
			l_lon_gps += ((int)incoming_message[38] - 48) * (long)1000;
			l_lon_gps += ((int)incoming_message[39] - 48) * (long)100;
			l_lon_gps += ((int)incoming_message[40] - 48) * (long)10;
			// To convert minutes to degrees we need to divide minutes by 6
			l_lon_gps /= (long)6;
			// Add multiply degrees by 10
			l_lon_gps += ((int)incoming_message[30] - 48) * (long)1000000000;
			l_lon_gps += ((int)incoming_message[31] - 48) * (long)100000000;
			l_lon_gps += ((int)incoming_message[32] - 48) * (long)10000000;
			// Divide everything by 10
			l_lon_gps /= 10;

			if (incoming_message[28] == 'S')
				// South latitude
				l_lat_gps *= -1;

			if (incoming_message[42] == 'W')
				// West longitude
				l_lon_gps *= -1;

			// Filter the number of satillites from the GGA line
			number_used_sats = ((int)incoming_message[46] - 48) * (long)10;
			number_used_sats += (int)incoming_message[47] - 48;

			// Get GPS altitude
			/*if (incoming_message[54] == '-')
				count_var = 55;
			else
				count_var = 54;
			gps_altitude_dm = (int)incoming_message[count_var] - 48;
			count_var++;
			for (count_var; count_var < 100; count_var++) {
				if (incoming_message[count_var] == ',')
					break;
				else if (incoming_message[count_var] != '.') {
					gps_altitude_dm *= 10;
					gps_altitude_dm += (int)incoming_message[count_var] - 48;
				}
			}
			if (incoming_message[54] == '-')
				gps_altitude_dm *= -1;*/

			gps_lost_counter = 0;
		}


		// If the line starts with SA and if there is a GPS fix we can scan the line for the fix type (none, 2D or 3D)
		if (incoming_message[4] == 'S' && incoming_message[5] == 'A')
			fix_type = (int)incoming_message[9] - 48;
	}
}
#endif