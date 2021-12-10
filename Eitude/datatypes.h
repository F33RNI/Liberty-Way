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

#ifndef DATATYPES_H
#define DATATYPES_H

// Common variables
uint8_t error;
uint16_t count_var;

// Communication
uint8_t check_byte, temp_byte;
uint8_t rx_buffer[6];
uint8_t rx_buffer_position;
uint8_t rx_byte_previous;
char tx_buffer[18];
boolean tx_flag;
#ifdef UDP_PORT
byte Ethernet::buffer[500];
boolean started;
#endif

// Data from Liberty-Way
uint8_t system_status;
uint8_t backlight_state;

// Grips system
uint8_t grips_command;

// Light sensor
uint16_t lux_raw_data;
#ifndef LUX_METER
float resistor_divider_voltage;
float ldr_resistance;
#endif
float unfiltered_lux, filtered_lux, converted_lux;
uint8_t lux_sqrt_data;
uint64_t lux_cycle_timer;

// GPS
#ifdef GPS
uint8_t gps_buffer[11];
uint8_t gps_buffer_position, gps_byte_previous;
uint8_t gps_check_byte, gps_temp_byte;
uint64_t gps_lost_timer;
int32_t l_lat_gps, l_lon_gps;
uint8_t number_used_sats;
uint8_t hdop;
int16_t altitude;
uint16_t ground_heading;
uint16_t ground_speed;
#endif

// WS2812
uint64_t leds_error_loop_timer;
uint8_t leds_tick_counter;
uint8_t leds_error_counter;

#endif
