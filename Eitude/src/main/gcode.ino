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

/// <summary>
/// Handles GCode commands
/// </summary>
void gcode_handler(void) {
	// M - commands
	command = gcode_parse('M', -1);
	switch (command)
	{
	case 3:
		// Enable tool (LED)
		digitalWrite(LIGHTS_PIN, 1);
		break;
	case 5:
		// Disable tool (LED)
		digitalWrite(LIGHTS_PIN, 0);
		break;
	default:
		break;
	}

	// L - commands
	command = gcode_parse('L', -1);
	switch (command)
	{
	case 0:
		// Illumination check
		Serial.print(F("S0 L"));
		Serial.println((uint16_t)filtered_value);
		break;
	case 1:
		// Speed check
		Serial.print(F("S0 L"));
		Serial.println(speed_accumulator, 2);
		break;
	case 2:
		// Set status
		switch ((int8_t)gcode_parse('S', -1))
		{
		case 0:
			// IDLE
			for (count_var = 0; count_var < STATUS_PIXELS_NUM; count_var++)
				ws_leds.setPixelColor(count_var, COLOR_IDLE);
			break;
		case 1:
			// STAB
			for (count_var = 0; count_var < STATUS_PIXELS_NUM; count_var++)
				ws_leds.setPixelColor(count_var, COLOR_STAB);
			break;
		case 2:
			// LAND
			for (count_var = 0; count_var < STATUS_PIXELS_NUM; count_var++)
				ws_leds.setPixelColor(count_var, COLOR_LAND);
			break;
		case 3:
			// PREV
			for (count_var = 0; count_var < STATUS_PIXELS_NUM; count_var++)
				ws_leds.setPixelColor(count_var, COLOR_PREV);
			break;
		case 4:
			// LOST
			for (count_var = 0; count_var < STATUS_PIXELS_NUM; count_var++)
				ws_leds.setPixelColor(count_var, COLOR_LOST);
			break;
		case 5:
			// TKOF
			for (count_var = 0; count_var < STATUS_PIXELS_NUM; count_var++)
				ws_leds.setPixelColor(count_var, COLOR_TKOF);
			break;
		case 6:
			// WAYP
			for (count_var = 0; count_var < STATUS_PIXELS_NUM; count_var++)
				ws_leds.setPixelColor(count_var, COLOR_WAYP);
			break;
		case 7:
			// DONE
			for (count_var = 0; count_var < STATUS_PIXELS_NUM; count_var++)
				ws_leds.setPixelColor(count_var, COLOR_DONE);
			break;
		default:
			// Other
			ws_leds.clear();
			break;
		}
		break;
	case 3:
		// Pressure check
		Serial.print(F("S0 P"));
		Serial.println(SIMULATE_PRESSURE);
		break;
	case 4:
		// GPS check
		Serial.print(F("S0 A"));
		Serial.print(SIMULATE_GPS_LAT);
		Serial.print(F(" O"));
		Serial.print(SIMULATE_GPS_LON);
		Serial.print(F(" N"));
		Serial.print(SIMULATE_GPS_SATS_N);
		Serial.print(F(" F"));
		Serial.println(SIMULATE_GPS_FIX_T);
		break;
	case 8:
		// Check status
		Serial.print(F("S"));
		Serial.println(error);
		break;
	default:
		break;
	}
}

/// <summary>
/// Parses GCode string
/// </summary>
/// <param name="code"> Symbol you want to know the meaning of </param>
/// <param name="val"> Value if the character is not found </param>
/// <returns> Value of the character </returns>
float gcode_parse(char code, float val) {
	char* ptr = buffer;
	while ((long)ptr > 1 && (*ptr) && (long)ptr < (long)buffer + sofar) {
		if (*ptr == code) {
			return atof(ptr + 1);
		}
		ptr = strchr(ptr, ' ') + 1;
	}
	return val;
}
