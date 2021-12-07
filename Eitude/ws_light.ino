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

#ifdef WS_LEDS

/// <summary>
/// Initializes WS2812B
/// </summary>
void ws_setup(void) {
	ws_leds.begin();
	ws_leds.clear();
	ws_leds.show();
}

/// <summary>
/// Shows current system state with WS2812 LEDs
/// </summary>
void ws_light(void) {
	if (error > 0) {
		// Error exists
		leds_error_signal();
	}
	else {
		// First LED - Liberty-Way status
		switch (system_status)
		{
		case STATUS_IDLE:
			// IDLE
			ws_leds.setPixelColor(0, COLOR_IDLE);
			break;
		case STATUS_WAYP:
			// WAYP
			ws_leds.setPixelColor(0, COLOR_WAYP);
			break;
		case STATUS_STAB:
			// STAB
			ws_leds.setPixelColor(0, COLOR_STAB);
			break;
		case STATUS_LAND:
			// LAND
			ws_leds.setPixelColor(0, COLOR_LAND);
			break;
		case STATUS_PREV:
			// PREV
			ws_leds.setPixelColor(0, COLOR_PREV);
			break;
		case STATUS_LOST:
			// LOST
			ws_leds.setPixelColor(0, COLOR_LOST);
			break;
		case STATUS_DONE:
			// DONE
			ws_leds.setPixelColor(0, COLOR_DONE);
			break;
		default:
			// Other
			ws_leds.setPixelColor(0, 0);
			break;
		}

		// Second LED - Grips state
		if (grips_command)
			ws_leds.setPixelColor(1, COLOR_GRIPS);
		else
			ws_leds.setPixelColor(1, 0);

		// Third LED - GPS state
#ifdef GPS
		if (millis() - gps_lost_timer < GPS_LOST_TIME && millis() > GPS_LOST_TIME)
			ws_leds.setPixelColor(2, COLOR_GPS);
		else
			ws_leds.setPixelColor(2, 0);
#else
		ws_leds.setPixelColor(2, 0);
#endif

		// Invert tick counter
		leds_tick_counter = !leds_tick_counter;
	}

	// Update up WS2812
	ws_leds.show();
}

/// <summary>
/// Blinks with COLOR_CALIBRATION. Useful to indicate the calibration process
/// </summary>
void leds_calibration_signal(void) {
	// Change flag
	leds_tick_counter = !leds_tick_counter;

	// Switch colors
	if (leds_tick_counter) {
		ws_leds.setPixelColor(0, COLOR_CALIBRATION);
		ws_leds.setPixelColor(1, COLOR_CALIBRATION);
		ws_leds.setPixelColor(2, COLOR_CALIBRATION);
	}
	else {
		ws_leds.clear();
	}
	ws_leds.show();
}

/// <summary>
/// Blinks with COLOR_ERROR as many times as the error is
/// </summary>
void leds_error_signal(void) {
	if (millis() - leds_error_loop_timer >= LEDS_ERROR_TIME) {
		// Reset loop timer
		leds_error_loop_timer = millis();

		// Reset leds_error_counter after +3 cycles (for delay)
		if (error > 0 && leds_error_counter > error + 3)
			leds_error_counter = 0;
		if (leds_error_counter < error && !leds_tick_counter && error > 0) {
			// Turn LEDs on if the error flash sequence isn't finished and the red LED is off
			ws_leds.setPixelColor(0, COLOR_ERROR);
			ws_leds.setPixelColor(1, COLOR_ERROR);
			ws_leds.setPixelColor(2, COLOR_ERROR);
			leds_tick_counter = 1;
		}
		else {
			// Turn LEDs off if the error flash sequence isn't finished and the red LED is on
			ws_leds.clear();
			leds_tick_counter = 0;

			// Increment counter
			leds_error_counter++;
		}

		// Update WS2812 LEDs
		ws_leds.show();
	}
}

#endif