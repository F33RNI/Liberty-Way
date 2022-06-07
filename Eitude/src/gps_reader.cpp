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
#include "gps_reader.h"

/**
 * @brief Initializes GPS-Mixer
 * 
 */
void gps_reader_setup(void) {
    // Open serial port
    GPS_MIXER_PORT.begin(GPS_MIXER_BAUD_RATE);
    delay(100);

    // Flush all data from buffer
    GPS_MIXER_PORT.flush();
}

/**
 * @brief Reads data from GPS-Mixer
 * 
 */
void gps_reader(void) {
    while (GPS_MIXER_PORT.available()) {
        // Read current byte
        gps_buffer[gps_buffer_position] = GPS_MIXER_PORT.read();

        if (gps_byte_previous == GPS_MIXER_SUFFIX_1 && gps_buffer[gps_buffer_position] == GPS_MIXER_SUFFIX_2) {
            // If data suffix appears
            // Reset buffer position
            gps_buffer_position = 0;

            // Reset check sum
            gps_check_byte = 0;

            // Calculate check sum
            for (gps_temp_byte = 0; gps_temp_byte <= 16; gps_temp_byte++)
                gps_check_byte ^= gps_buffer[gps_temp_byte];

            if (gps_check_byte == gps_buffer[17]) {
                // If the check sums are equal
                // Reset watchdog
                gps_lost_timer = millis();

                // GPS position
                l_lat_gps = (int32_t)gps_buffer[3] | (int32_t)gps_buffer[2] << 8 | (int32_t)gps_buffer[1] << 16 | (int32_t)gps_buffer[0] << 24;
                l_lon_gps = (int32_t)gps_buffer[7] | (int32_t)gps_buffer[6] << 8 | (int32_t)gps_buffer[5] << 16 | (int32_t)gps_buffer[4] << 24;

                // Number of satellites
                number_used_sats = gps_buffer[9];

                // HDOP (multiplied by 10)
                hdop = gps_buffer[10];

                // Altitude (multiplied by 10)
                altitude = (int16_t)gps_buffer[12] | (int16_t)gps_buffer[11] << 8;

                // Ground heading (multiplied by 100)
                ground_heading = (uint16_t)gps_buffer[14] | (uint16_t)gps_buffer[13] << 8;

                // Ground speed (multiplied by 10)
                ground_speed = (uint16_t)gps_buffer[16] | (uint16_t)gps_buffer[15] << 8;
            }
        }
        else {
            // Store data bytes
            gps_byte_previous = gps_buffer[gps_buffer_position];
            gps_buffer_position++;

            // Reset buffer on overflow
            if (gps_buffer_position >= 20)
                gps_buffer_position = 0;
        }
    }

    // When there is no GPS information available
    if (millis() - gps_lost_timer > GPS_MIXER_LOST_TIME || millis() <= GPS_MIXER_LOST_TIME) {
        // Reset some variables
        l_lat_gps = 0;
        l_lon_gps = 0;
        number_used_sats = 0;

        // Show error
        if (!error)
            error = ERROR_NO_GPS;
    }

    // Clear ERROR_NO_GPS if GPS appears again
    else if (error == ERROR_NO_GPS)
        error = 0;
}