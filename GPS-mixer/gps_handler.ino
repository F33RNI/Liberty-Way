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
/// Reads serial data to gps_buffer
/// </summary>
/// <param name="gps_serial"></param>
/// <param name="serial_container_"></param>
/// <param name="gps_container_"></param>
void gps_read(HardwareSerial* gps_serial, serial_container* serial_container_, gps_container* gps_container_) {
    // Check for new data
    while (gps_serial->available()) {
        // Check timeout
        if (timeout_started_flag && millis() - timeout_timer >= TIMEOUT_TIME) {
            reset_gps_container(gps_container_);
            reset_serial_container(serial_container_);
            break;
        }

        // Fill current byte
        serial_container_->gps_buffer[serial_container_->gps_buffer_position] = gps_serial->read();

        // End of line (character LF)
        if (serial_container_->gps_buffer[serial_container_->gps_buffer_position] == '\n') {

            // Read and parse current line
            nmea_read(serial_container_, gps_container_);

            // Reset buffer position
            serial_container_->gps_buffer_position = 0;

            // Check new data
            if (gps_container_->new_gga_available && gps_container_->new_vtg_available
                && gps_container_->sats_num > 3 && gps_container_->quality && gps_container_->hdop < 90.0
                && !gps_container_->is_working) {
                gps_container_->is_working = 1;
                working_receivers_num++;
            }
        }
        // Append to the buffer
        else {
            // Increment buffer position
            serial_container_->gps_buffer_position++;

            // Buffer overflow
            if (serial_container_->gps_buffer_position == GPS_BUFFER_SIZE) {
                reset_gps_container(gps_container_);
                reset_serial_container(serial_container_);
            }
        }
    }
}

/// <summary>
/// Combines data from multiple receivers
/// </summary>
/// <param name=""></param>
void combine_new_data(void) {
    // Reset temp variables
    l_lat_avg = 0;
    l_lon_avg = 0;
    gps_container_avg.lat = 0;
    gps_container_avg.lon = 0;
    gps_container_avg.quality = 0;
    gps_container_avg.sats_num = 0;
    gps_container_avg.hdop = 0;
    gps_container_avg.altitude = 0;
    gps_container_avg.ground_heading = 0;
    gps_container_avg.ground_speed = 0;
    receivers_changed = 0;

    // If there is at least 1 receiver
    if (working_receivers_num) {
        // Calculate total sum
#ifdef GPS_1_RX_SERIAL
        append_to_sum(&gps_container_1);
#endif
#ifdef GPS_2_RX_SERIAL
        append_to_sum(&gps_container_2);
#endif
#ifdef GPS_3_RX_SERIAL
        append_to_sum(&gps_container_3);
#endif

        // Calculate average values
        gps_container_avg.lat /= (double)working_receivers_num;
        gps_container_avg.lon /= (double)working_receivers_num;
        gps_container_avg.quality /= (double)working_receivers_num;
        gps_container_avg.sats_num /= (double)working_receivers_num;
        gps_container_avg.hdop /= (double)working_receivers_num;
        gps_container_avg.altitude /= (double)working_receivers_num;
        gps_container_avg.ground_heading /= (double)working_receivers_num;
        gps_container_avg.ground_speed /= (double)working_receivers_num;

        // Calculate deviations if number of available receivers changed
        if (working_receivers_num_last > 0 && receivers_changed) {
            d_lat = avg_lat_prev - gps_container_avg.lat;
            d_lon = avg_lon_prev - gps_container_avg.lon;
        }
        
        // Smoothly reduce corrections
        d_lat *= D_CORRECTION_TERM;
        d_lon *= D_CORRECTION_TERM;

        // Add corrections
        gps_container_avg.lat += d_lat;
        gps_container_avg.lon += d_lon;

        // Store variables for smooting
        avg_lat_prev = gps_container_avg.lat;
        avg_lon_prev = gps_container_avg.lon;

        // Take degrees
        l_lat_avg = (int32_t)gps_container_avg.lat / 100;
        l_lon_avg = (int32_t)gps_container_avg.lon / 100;

        // Remove degrees from double
        gps_container_avg.lat -= l_lat_avg * 100;
        gps_container_avg.lon -= l_lon_avg * 100;

        // Convert minutes to degrees
        gps_container_avg.lat /= 60;
        gps_container_avg.lon /= 60;

        // Add degrees
        gps_container_avg.lat += l_lat_avg;
        gps_container_avg.lon += l_lon_avg;

        // Convert to integer
        l_lat_avg = gps_container_avg.lat * 1000000.0;
        l_lon_avg = gps_container_avg.lon * 1000000.0;
    }

    // Check and convert hdop to integer
    if (gps_container_avg.hdop < 0)
        hdop = 0;
    else if (gps_container_avg.hdop * 10.0 > UINT8_MAX)
        hdop = UINT8_MAX;
    else
        hdop = gps_container_avg.hdop * 10.0;

    // Check and convert altitude to integer
    if (gps_container_avg.altitude * 10.0 < INT16_MIN)
        altitude = INT16_MIN;
    else if (gps_container_avg.altitude * 10.0 > INT16_MAX)
        altitude = INT16_MAX;
    else
        altitude = gps_container_avg.altitude * 10.0;

    // Check and convert ground heading to integer
    if (gps_container_avg.ground_heading < 0
        || gps_container_avg.ground_heading * 100.0 > 36000.0)
        ground_heading = 0;
    else
        ground_heading = gps_container_avg.ground_heading * 100.0;

    // Check and convert ground speed to integer
    if (gps_container_avg.ground_speed < 0)
        ground_speed = 0;
    else if (gps_container_avg.ground_speed * 10.0 > UINT16_MAX)
        ground_speed = UINT16_MAX;
    else
        ground_speed = gps_container_avg.ground_speed * 10.0;

    // Fill buffer with new data
    output_buffer[0] = l_lat_avg >> 24;
    output_buffer[1] = l_lat_avg >> 16;
    output_buffer[2] = l_lat_avg >> 8;
    output_buffer[3] = l_lat_avg;
    output_buffer[4] = l_lon_avg >> 24;
    output_buffer[5] = l_lon_avg >> 16;
    output_buffer[6] = l_lon_avg >> 8;
    output_buffer[7] = l_lon_avg;
    output_buffer[8] = gps_container_avg.quality;
    output_buffer[9] = gps_container_avg.sats_num;
    output_buffer[10] = hdop;
    output_buffer[11] = altitude >> 8;
    output_buffer[12] = altitude;
    output_buffer[13] = ground_heading >> 8;
    output_buffer[14] = ground_heading;
    output_buffer[15] = ground_speed >> 8;
    output_buffer[16] = ground_speed;

    // Calculate check-sum
    output_buffer[17] = 0;
    for (count_var = 0; count_var <= 16; count_var++)
        output_buffer[17] ^= output_buffer[count_var];
}

/// <summary>
/// Checks if data is available from this receiver, 
/// if yes, adds it to the total to calculate the average
/// </summary>
void append_to_sum(gps_container* container) {
    // Check if receiver's state changed
    if (container->is_working != container->is_working_last)
        receivers_changed = 1;

    // Store receiver's state for next cycle
    container->is_working_last = container->is_working;

    // Check if new data is available
    if (container->is_working) {
        // Add new data to the sum to calculate the average
        gps_container_avg.lat += container->lat;
        gps_container_avg.lon += container->lon;
        gps_container_avg.quality += container->quality;
        gps_container_avg.sats_num += container->sats_num;
        gps_container_avg.hdop += container->hdop;
        gps_container_avg.altitude += container->altitude;
        gps_container_avg.ground_heading += container->ground_heading;
        gps_container_avg.ground_speed += container->ground_speed;
    }
}

/// <summary>
/// Resets gps container for new data
/// </summary>
void reset_gps_container(gps_container* container) {
    container->new_gga_available = 0;
    container->new_vtg_available = 0;
    container->is_working = 0;
    container->sats_num = 0;
    container->quality = 0;
    container->hdop = 99.99;
}

/// <summary>
/// Resets serial container for new data
/// </summary>
void reset_serial_container(serial_container* container) {
    container->gps_buffer_position = 0;
    container->nmea_buffer_position = 0;
    container->nmea_message_buffer_position = 0;
    container->nmea_message_index = 0;
}