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

#ifndef CONSTANTS_H
#define CONSTANTS_H

// Max variables values
#define UINT16_MAX	65535

// LCD constants
const char* empty_line = "                ";

// Hardware constants
#ifdef LUX_METER
const uint8_t LUX_METER_ADDRESS PROGMEM = 0x23;
#endif
#ifdef BAROMETER
const uint8_t BAROMETER_ADDRESS PROGMEM = 0x77;
#endif

// Loop frequency is 200 Hz. Changing it may cause injury
const uint32_t LOOP_PERIOD = 4000;
const uint32_t MAX_ALLOWED_LOOP_PERIOD = 4500;


// GPS UBLOX predefined messages for setup
const uint8_t GPS_DISABLE_GPGSV[11] = { 0xB5, 0x62, 0x06, 0x01, 0x03, 0x00, 0xF0, 0x03, 0x00, 0xFD, 0x15 };
const uint8_t GPS_SET_TO_5HZ[14]  = { 0xB5, 0x62, 0x06, 0x08, 0x06, 0x00, 0xC8, 0x00, 0x01, 0x00, 0x01, 0x00, 0xDE, 0x6A };
const uint8_t GPS_SET_TO_57KBPS[28] = { 0xB5, 0x62, 0x06, 0x00, 0x14, 0x00, 0x01, 0x00, 0x00, 0x00, 0xD0, 0x08, 0x00, 0x00,
0x00, 0xE1, 0x00, 0x00, 0x07, 0x00, 0x07, 0x00, 0x00, 0x00, 0x00, 0x00, 0xE2, 0xE1 };

#endif