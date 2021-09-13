/*
 * Copyright (C) 2021 Fern H. (Pavel Neshumov), Liberty Drones GPS mixer
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

/// <summary>
/// Collects NMEA messages from gps_buffer
/// </summary>
/// <param name="serial_container_"></param>
/// <param name="gps_container_"></param>
void nmea_read(serial_container* serial_container_, gps_container* gps_container_) {
    // Check if the first symbol is $ and the last is New line
    if (serial_container_->gps_buffer[0] == '$' 
        && serial_container_->gps_buffer[serial_container_->gps_buffer_position] == '\n') {
        // Reset NMEA buffer position
        serial_container_->nmea_message_buffer_position = 0;

        // Reset NMEA message index
        serial_container_->nmea_message_index = 0;

        // Reset NMEA checksum
        serial_container_->nmea_checksum = 0;

        // Parse all bytes
        for (serial_container_->nmea_buffer_position = 1; 
            serial_container_->nmea_buffer_position < serial_container_->gps_buffer_position; 
            serial_container_->nmea_buffer_position++) {
            // Calculate checksum
            if (serial_container_->nmea_buffer_position < serial_container_->gps_buffer_position - 4)
                serial_container_->nmea_checksum ^= serial_container_->gps_buffer[serial_container_->nmea_buffer_position];

            // New NMEA message complete
            if (serial_container_->gps_buffer[serial_container_->nmea_buffer_position] == ','
                || serial_container_->gps_buffer[serial_container_->nmea_buffer_position] == '*'
                || serial_container_->gps_buffer[serial_container_->nmea_buffer_position] == 0x0D) {
                // Reset tail of the message
                for (serial_container_->nmea_message_buffer_position_temp = serial_container_->nmea_message_buffer_position;
                    serial_container_->nmea_message_buffer_position_temp < NMEA_MESSAGE_SIZE;
                    serial_container_->nmea_message_buffer_position_temp++)
                    serial_container_->nmea_message_buffer[serial_container_->nmea_message_buffer_position] = 0;

                // Parse NMEA message
                nmea_parse(serial_container_, gps_container_);

                // Increment NMEA message index and reset NMEA buffer position
                serial_container_->nmea_message_index++;
                serial_container_->nmea_message_buffer_position = 0;
            }
            else {
                // Collecting new NMEA message
                serial_container_->nmea_message_buffer[serial_container_->nmea_message_buffer_position] =
                    serial_container_->gps_buffer[serial_container_->nmea_buffer_position];
                serial_container_->nmea_message_buffer_position++;

                // Buffer overflow
                if (serial_container_->nmea_message_buffer_position == NMEA_MESSAGE_SIZE) {
                    reset_gps_container(gps_container_);
                    reset_serial_container(serial_container_);
                }
            }
        }
    }
}

/// <summary>
/// Parses NMEA messages
/// </summary>
/// <param name="serial_container_"></param>
/// <param name="gps_container_"></param>
void nmea_parse(serial_container* serial_container_, gps_container* gps_container_) {
    // GGA message
    if (serial_container_->gps_buffer[3] == 'G'
        && serial_container_->gps_buffer[4] == 'G'
        && serial_container_->gps_buffer[5] == 'A') {
        // Latitude
        if (serial_container_->nmea_message_index == 2)
            gps_container_->lat = atof(serial_container_->nmea_message_buffer);

        // South latitude
        else if (serial_container_->nmea_message_index == 3) {
            if (serial_container_->nmea_message_buffer[0] == 'S')
                gps_container_->lat *= -1;
        }

        // Longitude
        else if (serial_container_->nmea_message_index == 4)
            gps_container_->lon = atof(serial_container_->nmea_message_buffer);

        // West longitude
        else if (serial_container_->nmea_message_index == 5) {
            if (serial_container_->nmea_message_buffer[0] == 'W')
                gps_container_->lon *= -1;
        }

        // Quality
        else if (serial_container_->nmea_message_index == 6)
            gps_container_->quality = ((int)serial_container_->nmea_message_buffer[0] - 48);

        // Number of satellites
        else if (serial_container_->nmea_message_index == 7)
            gps_container_->sats_num = ((int)serial_container_->nmea_message_buffer[0] - 48) * 10
            + ((int)serial_container_->nmea_message_buffer[1] - 48);

        // HDOP
        else if (serial_container_->nmea_message_index == 8)
            gps_container_->hdop = atof(serial_container_->nmea_message_buffer);

        // Altitude above MSL
        else if (serial_container_->nmea_message_index == 9) {
            if (serial_container_->nmea_message_buffer_position > 0)
                gps_container_->altitude = atof(serial_container_->nmea_message_buffer);
        }

        // Check-sum
        else if (serial_container_->nmea_message_index == 15) {
            if (strtol(serial_container_->nmea_message_buffer, 0, 16) == serial_container_->nmea_checksum)
                gps_container_->new_gga_available = 1;
        }
    }

    // VTG message
    if (serial_container_->gps_buffer[3] == 'V' 
        && serial_container_->gps_buffer[4] == 'T' 
        && serial_container_->gps_buffer[5] == 'G') {
        // Course over ground
        if (serial_container_->nmea_message_index == 1) {
            // Change ground heading only if message is not empty
            if (serial_container_->nmea_message_buffer_position > 0)
                gps_container_->ground_heading = atof(serial_container_->nmea_message_buffer);
        }

        // Ground speed
        else if (serial_container_->nmea_message_index == 7)
            gps_container_->ground_speed = atof(serial_container_->nmea_message_buffer);

        // Check-sum
        else if (serial_container_->nmea_message_index == 10) {
            if (strtol(serial_container_->nmea_message_buffer, 0, 16) == serial_container_->nmea_checksum) {
                gps_container_->new_vtg_available = 1;
            }
        }
    }
}