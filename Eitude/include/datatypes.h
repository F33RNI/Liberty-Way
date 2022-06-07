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

#ifndef DATATYPES_H
#define DATATYPES_H

// Libraries
#include <Arduino.h>
#include <Wire.h>

// Methods
void status_led_setup(void);
void status_led(void);
void backlight_setup(void);
void backlight(void);
void communication_setup(void);
void communication_ether_packet_loop(void);
void motors_setup(void);
void motors_home(void);
void motors_alignment_system(void);
void udp_receive_data(uint16_t dest_port, uint8_t src_ip[2], uint16_t src_port, const char* data, uint16_t len);
void parse_byte(uint8_t incoming_data);
void data_send(void);
void gps_reader_setup(void);
void gps_reader(void);
void lux_meter_setup(void);
void lux_meter(void);

// Common variables
extern volatile uint8_t error;

// Data from Liberty-Way
extern volatile uint8_t system_status;
extern volatile uint8_t backlight_state;
extern volatile uint8_t alignment_state;

// Data from GPS-Mixer
extern volatile int32_t l_lat_gps, l_lon_gps;
extern volatile uint8_t number_used_sats;
extern volatile uint8_t hdop;
extern volatile int16_t altitude;
extern volatile uint16_t ground_heading;
extern volatile uint16_t ground_speed;

// Compressed LUX data from lux-meter
extern volatile uint8_t lux_sqrt_data;

// Error codes
#define ERROR_MOTORS_SETUP 1
#define ERROR_MOTORS_HOME 2
#define ERROR_ETHERNET_CONTROLLER 3
#define ERROR_IP 4
#define ERROR_LUX_METER 5
#define ERROR_NO_GPS 6

#endif