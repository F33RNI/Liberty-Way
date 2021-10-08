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

#ifndef CONFIG_H
#define CONFIG_H

/***************************************/
/*            Communication            */
/***************************************/
// Comment if using serial port instead of ENC28J60 (Ethernet UDP port)
#define UDP_PORT	8888

#ifdef UDP_PORT
// Ethernet MAC address
static byte MAC[] = { 0x70, 0x69, 0x69, 0x2D, 0x30, 0x31 };

// Static IP
static byte STATIC_IP[] = { 192, 168, 9, 185 };

// Gateway IP
static byte GATEWAY_IP[] = { 192, 168, 9, 1 };

// Gateway IP
static byte DNS_IP[] = { 8, 8, 8, 8 };

// Gateway IP
static byte MASK[] = { 255, 255, 255, 0 };

#else
// Communication serial port
#define COMMUNICATION_SERIAL	Serial

// Serial port speed
const uint32_t COMMUNICATION_BAUDRATE = 57600;
#endif

// Unique pair of HEX symbols as packet ending
const uint8_t PACKET_SUFFIX_1 PROGMEM = 0xEE;
const uint8_t PACKET_SUFFIX_2 PROGMEM = 0xEF;



/**************************************/
/*            Light sensor            */
/**************************************/
// Comment if using analog LDR sensor
//#define LUX_METER

// LDR constants
// Measure this for best results
#ifndef LUX_METER
#define ADC_REF_VOLTAGE 4.97
#define REF_RESISTANCE 10020 
#define LUX_CALC_SCALAR 37000000 //12518931
#define LUX_CALC_EXPONENT -1.350 //-1.405
#endif

// Request LUX value every 25 * 4ms = 100ms
const uint8_t LUX_REQUST_CYCLES PROGMEM = 25;


/******************************/
/*            Pins            */
/******************************/
// Analog LDR sensor
#ifndef LUX_METER
const uint8_t LDR_PIN PROGMEM = A1;
#endif

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


/*****************************/
/*            GPS            */
/*****************************/
// Comment to disable GPS module
#define GPS

#ifdef GPS
// GPS serial port
#define GPS_SERIAL	Serial

// Baud rate of GPS mixer serial port
const uint32_t GPS_BAUD_RATE = 115200;

// If no data in 750 * 4ms = 3000ms the gps will be considered lost
const uint16_t GPS_LOST_CYCLES PROGMEM = 750;

// Unique pair of suffix
const uint8_t GPS_SUFFIX_1 PROGMEM = 0xEE;
const uint8_t GPS_SUFFIX_2 PROGMEM = 0xEF;
#endif


/***********************************/
/*            Barometer            */
/***********************************/
// Comment to disable barometer module
#define BAROMETER

// This value will be added to the pressure. Make it equal to the pressure from the drone
#ifdef BAROMETER
const float PRESSURE_CORRECTION = 48;
#endif



/*************************************/
/*            WS2812 LEDs            */
/*************************************/
// Comment to disable ws2812 strip
#define WS_LEDS

#ifdef WS_LEDS
// Change state every 63 * LOOP_PERIOD for error signal
const uint16_t LEDS_ERROR_CYCLES PROGMEM = 63;

// LED colors
#define COLOR_CALIBRATION		63, 0, 255
#define COLOR_ERROR				255, 110, 0
#define COLOR_GRIPS				0, 255, 0
#define COLOR_GPS				0, 127, 255

#define COLOR_IDLE				127, 0, 127
#define COLOR_STAB				0, 255, 0
#define COLOR_LAND				0, 255, 255
#define COLOR_PREV				255, 127, 0
#define COLOR_LOST				255, 0, 0
#define COLOR_WAYP				63, 63, 63
#define COLOR_DONE				127, 127, 127
#endif

#endif
