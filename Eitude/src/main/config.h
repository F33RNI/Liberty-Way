/*
 * Copyright (C) 2021 Frey Hertz (Pavel Neshumov), AMLS Platform controller
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

#ifndef CONFIG_H
#define CONFIG_H

/******************************/
/*            Pins            */
/******************************/
// Analog light sensors
const uint8_t LIGHT_S_1_PIN PROGMEM = A0;
const uint8_t LIGHT_S_2_PIN PROGMEM = A1;

// LCD
const uint8_t LDC_RS_PIN PROGMEM = 2;
const uint8_t LDC_E_PIN PROGMEM = 3;
const uint8_t LCD_D4_PIN PROGMEM = 4;
const uint8_t LCD_D5_PIN PROGMEM = 5;
const uint8_t LCD_D6_PIN PROGMEM = 6;
const uint8_t LCD_D7_PIN PROGMEM = 7;

// Backlight
const uint8_t LIGHTS_PIN PROGMEM = 9;

// WS2812 Strip
const uint8_t STATUS_STRIP_PIN PROGMEM = 8;


/*************************************/
/*            Serial port            */
/*************************************/
// Serial port speed
const uint32_t BAUD_RATE PROGMEM = 57600;


/***************************************/
/*            Light sensors            */
/***************************************/
// Light Kalman filter
const float LUX_FILTER_KOEFF PROGMEM = 0.95;


/************************************/
/*            Simulation            */
/************************************/
// Simulate GPS data (L4 command) with this coordinates
const int32_t SIMULATE_GPS_LAT PROGMEM = 8983272;
const int32_t SIMULATE_GPS_LON PROGMEM = -79559480;
const uint8_t SIMULATE_GPS_SATS_N PROGMEM = 12;
const uint8_t SIMULATE_GPS_FIX_T PROGMEM = 3;

// Simulate platform pressure (L3 command) with this value
const uint32_t SIMULATE_PRESSURE PROGMEM = 100780;


/*****************************/
/*            LCD            */
/*****************************/
// Print data to LCD every ms
const uint16_t LCD_PRINT_TIME PROGMEM = 500;


/*****************************/
/*            IMU            */
/*****************************/
const uint16_t CALIBRATION_N PROGMEM = 500;
const float ACC_FILTER_KOEFF PROGMEM = 0.95;
const float VIBR_STOP_MOVE_THRESH PROGMEM = 300;
const float SPEED_ZEROING_FACTOR PROGMEM = 0.98;


/*************************************/
/*            WS2812 LEDs            */
/*************************************/
// Number of LEDs
const uint16_t STATUS_PIXELS_NUM PROGMEM = 3;

// LED colors
#define COLOR_IDLE				127, 0, 127
#define COLOR_STAB				0, 255, 0
#define COLOR_LAND				0, 255, 255
#define COLOR_PREV				255, 127, 0
#define COLOR_LOST				255, 0, 0
#define COLOR_TKOF				0, 0, 255
#define COLOR_WAYP				63, 63, 63
#define COLOR_DONE				127, 127, 127

#endif
