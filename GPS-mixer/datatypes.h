/*
 * Copyright (C) 2022 Fern Lane, GPS-Mixer system
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

#ifndef DATATYPES_H
#define DATATYPES_H

// Common variables
uint16_t count_var;
uint8_t working_receivers_num, working_receivers_num_last;
boolean timeout_started_flag;
uint32_t timeout_timer;
uint32_t lost_timer;

// LED variables
boolean led_state;
uint8_t led_receivers_counter;
uint32_t led_timer;

// Serial variables
boolean new_data_flag;
uint8_t output_buffer[20];
uint8_t output_buffer_position;
uint32_t output_timer_micros;

// GPS variables
int32_t l_lat_avg, l_lon_avg;
uint8_t hdop;
int16_t altitude;
uint16_t ground_heading;
uint16_t ground_speed;
double avg_lat_prev, avg_lon_prev;
double d_lat, d_lon;
boolean receivers_changed;

struct serial_container
{
    uint8_t gps_buffer[GPS_BUFFER_SIZE];
    uint8_t gps_buffer_position, nmea_buffer_position;
    char nmea_message_buffer[NMEA_MESSAGE_SIZE];
    uint8_t nmea_message_buffer_position, nmea_message_buffer_position_temp, nmea_message_index, nmea_checksum;
};

struct gps_container
{
    boolean new_gga_available;
    boolean new_vtg_available;
    boolean is_working, is_working_last;
    double lat, lon;
    uint8_t quality;
    uint8_t sats_num;
    double hdop = 99.99;
    double altitude;
    double ground_heading;
    double ground_speed;
};

gps_container gps_container_avg;

#ifdef GPS_1_RX_SERIAL
gps_container gps_container_1;
serial_container serial_container_1;
#endif
#ifdef GPS_2_RX_SERIAL
gps_container gps_container_2;
serial_container serial_container_2;
#endif
#ifdef GPS_3_RX_SERIAL
gps_container gps_container_3;
serial_container serial_container_3;
#endif

uint8_t gps_cfg_id;

uint8_t GPS_CFG_BUFFER[11] = { 0xB5, 0x62, 0x06, 0x01, 0x03, 0x00, 0xF0, 0x00, 0x00, 0x00, 0x00 };

#endif