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

#ifndef CONFIG_H
#define CONFIG_H

/**********************************/
/*            IDE Pins            */
/**********************************/
// Status LED pin
#define PIN_STATUS_LED 2

// LEDs backlight pin
#define PIN_BACKLIGHT 3

// End switch pin
#define PIN_END_SWITCH 4

// Stepper drivers enable pin
#define PIN_ALIGNMENT_STEPPER_EN 7

// Stepper drivers step pin
#define PIN_ALIGNMENT_STEPPER_STEP 9

// Stepper drivers direction pin
#define PIN_ALIGNMENT_STEPPER_DIR 8


/***********************************/
/*            Backlight            */
/***********************************/
// PWM value for backlight brightness
const uint8_t BACKLIGHT_BRIGHTNESS PROGMEM = 100;


/***************************************/
/*            Communication            */
/***************************************/
// UDP port number
#define UDP_PORT 8888

// Ethernet MAC address
static byte MAC[] = { 0x70, 0x69, 0x69, 0x2D, 0x30, 0x31 };

// Static IP
static byte STATIC_IP[] = { 192, 168, 9, 185 };

// Unique pair of HEX symbols as packet ending
const uint8_t PACKET_SUFFIX_1 PROGMEM = 0xEE;
const uint8_t PACKET_SUFFIX_2 PROGMEM = 0xEF;


/***********************************/
/*            GPS-Mixer            */
/***********************************/
// Serial port for GPS-Mixer
#define GPS_MIXER_PORT Serial

// GPS-Mixer baud rate
#define GPS_MIXER_BAUD_RATE 115200

// GPS-Mixer packet ending
const uint8_t GPS_MIXER_SUFFIX_1 PROGMEM = 0xEE;
const uint8_t GPS_MIXER_SUFFIX_2 PROGMEM = 0xEF;

// If no data in 3000ms the gps will be considered lost
const uint64_t GPS_MIXER_LOST_TIME PROGMEM = 3000;


/*****************************************************/
/*            Mechanical alignment system            */
/*****************************************************/
// Acceleration of the steppers (steps / s^2)
#define STEPPER_ACCELERATION 1000

// Speed of the steppers (steps / s)
#define STEPPER_SPEED 400

// Number of steps from home position to close alignment system
#define STEPS_FOR_CLOSE 1600

// Waiting time (in ms) during which the alignment system must return to its home position
#define STEPPER_HOME_TIMEOUT 5000


/************************************/
/*            Status LED            */
/************************************/
// Time of one LED cycle (blinking speed)
const uint32_t LED_CYCLE_TIME PROGMEM = 200;


/***********************************/
/*            LUX meter            */
/***********************************/
// BH1750 I2C address
#define LUX_METER_ADDRESS 0x23

// Request data from sensor every LUX_REQUEST_TIME ms
const uint64_t LUX_REQUEST_TIME PROGMEM = 100;

#endif