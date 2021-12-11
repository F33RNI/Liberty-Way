/*
 * Copyright (C) 2021 Fern H. (aka Pavel Neshumov), Liberty Drones GPS mixer
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

#ifndef CONFIG_H
#define CONFIG_H

// Connected receivers. Comment any line if this receiver is not connected
#define GPS_1_RX_SERIAL Serial3
#define GPS_2_RX_SERIAL Serial2
#define GPS_3_RX_SERIAL Serial1

// Serial port through which the GPS receiver configuration commands are sent
#define GPS_TX_SERIAL Serial1

// Serial port on which data is sent to the host
#define OUTPUT_SERIAL Serial3

// Number of connected receivers
#if (defined(GPS_1_RX_SERIAL) && defined(GPS_2_RX_SERIAL) && defined(GPS_3_RX_SERIAL))
#define CONNECTED_RECEIVERS 3
#elif (defined(GPS_1_RX_SERIAL) && defined(GPS_2_RX_SERIAL))
#define CONNECTED_RECEIVERS 2
#elif (defined(GPS_2_RX_SERIAL) && defined(GPS_3_RX_SERIAL))
#define CONNECTED_RECEIVERS 2
#elif (defined(GPS_1_RX_SERIAL) && defined(GPS_3_RX_SERIAL))
#define CONNECTED_RECEIVERS 2
#elif (defined(GPS_1_RX_SERIAL) || defined(GPS_2_RX_SERIAL) || defined(GPS_3_RX_SERIAL))
#define CONNECTED_RECEIVERS 1
#else
#define CONNECTED_RECEIVERS 0
#endif

// Buffer sizes
#define GPS_BUFFER_SIZE 100
#define NMEA_MESSAGE_SIZE 100

// Delay (in us) between sending one byte from the buffer. Total time (delay * 20) should not exceed 50ms
// Set to 0 to send all bytes simultaneously
const uint32_t TRANSMIT_DELAY PROGMEM = 0; // 1200

// How many milliseconds to wait for a signal from other receivers, 
// when the signal appeared from the first available receiver
const uint32_t TIMEOUT_TIME PROGMEM = 50;

// In the case of the appearance / loss of some receivers, 
// there will be strong changes in the arithmetic mean.
// In order to smooth out these changes, when receivers are lost / found, 
// the difference between the values is calculated
// The closer D_CORRECTION_TERM is to 0, the sharper the transition will be
const double D_CORRECTION_TERM PROGMEM = 0.985;

// Time of one LED cycle
const uint32_t LED_CYCLE_TIME PROGMEM = 200;

// If data is not received from at least one receiver during GPS_LOST_TIME ms, 
// it is considered that the GPS signal is lost
const uint32_t GPS_LOST_TIME PROGMEM = 200;

// Unique pair of HEX symbols as packet ending
const uint8_t PACKET_SUFFIX_1 PROGMEM = 0xEE;
const uint8_t PACKET_SUFFIX_2 PROGMEM = 0xEF;

#endif
