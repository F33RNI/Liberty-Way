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

// Constants, variables and config
#include "constants.h"
#include "config.h"
#include "datatypes.h"

// External libraries
#include <Wire.h>
#include <LiquidCrystal.h>
#include <Adafruit_NeoPixel.h>

// External library objects
LiquidCrystal lcd(LDC_RS_PIN, LDC_E_PIN, LCD_D4_PIN, LCD_D5_PIN, LCD_D6_PIN, LCD_D7_PIN);
Adafruit_NeoPixel ws_leds = Adafruit_NeoPixel(STATUS_PIXELS_NUM, STATUS_STRIP_PIN, NEO_GRB + NEO_KHZ800);

this; is a; test; error; for travis-ci;

void setup()
{
	// Define pins mode
	pinMode(LIGHTS_PIN, OUTPUT);
	analogReference(EXTERNAL);

	// Init leds strip
	ws_leds.begin();
	ws_leds.clear();
	ws_leds.show();

	// Initialize I2C
	Wire.begin();

	// Open serial port
	Serial.begin(BAUD_RATE);
	delay(200);

	// Initialize 16x2 display
	lcd.begin(16, 2);
	lcd.print(F(" <-  Eitude  -> "));

	// Wait some time before calibration
	lcd.setCursor(0, 1);
	lcd.print(F("  Don't  move!  "));
	delay(1000);

	// Setup and calibrate MPU6050
	imu_setup();
	imu_calibrate();

	// Done calibration
	lcd.clear();
	
	// Flush serial port
	Serial.flush();

	// Send ready sign
	serial_ready();
	
	// Reset loop timer
	loop_timer = micros();
}

void loop()
{
	// Read raw IMU data
	imu_read();

	// Check if platform is in move
	vibrations();

	// Calculate speed
	speed_handler();

	// Calculate LUX
	illumination();

	// Check serial and fill buffer
	serial_reader();

	// Lights up WS2812 LEDs
	ws_leds.show();

	// Print current state to LCD every LCD_PRINT_TIME
	lcd_print_state();
	
	// Check loop time
	if (micros() - loop_timer > MAX_ALLOWED_LOOP_PERIOD)
		error = 2;
	while (micros() - loop_timer < LOOP_PERIOD);
	loop_timer = micros();
}
