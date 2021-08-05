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

#ifdef LUX_METER

/// <summary>
/// Initializes BH1750 Lux meter
/// </summary>
void lux_meter_setup(void) {
	// Check if the BH1750 is responding
	Wire.beginTransmission(LUX_METER_ADDRESS);

	// Power on BH1750
	Wire.write(0x01);

	error = Wire.endTransmission();
	while (error != 0) {
		// Stay in the loop because the lux meter did not responde
		error = 4;

		// Show curent error
		leds_error_signal();

		// Simulate main loop
		delayMicroseconds(LOOP_PERIOD);
	}

	// Reset BH1750
	Wire.beginTransmission(LUX_METER_ADDRESS);
	Wire.write(0x07);
	Wire.endTransmission();

	// Power down BH1750
	Wire.beginTransmission(LUX_METER_ADDRESS);
	Wire.write(0x00);
	Wire.endTransmission();

	// Wait some time
	delay(100);

	// Power on BH1750
	Wire.beginTransmission(LUX_METER_ADDRESS);
	Wire.write(0x01);
	Wire.endTransmission();

	// Set MTreg to 31 (lowest sensitivity)
	Wire.beginTransmission(LUX_METER_ADDRESS);
	Wire.write(0x40);
	Wire.write(0x7F);
	Wire.endTransmission();

	// Select continuously L-resolution mode
	Wire.beginTransmission(LUX_METER_ADDRESS);
	Wire.write(0x13);
	Wire.endTransmission();

	// Wait some time to complete first measurement
	delay(100);
}
#endif

/// <summary>
/// Reads illumination from the BH1750 sensor or from the LDR
/// </summary>
void lux_meter(void) {
	// Check illumination every LUX_CHECK_TIME ms
	if (millis() - lux_timer >= LUX_CHECK_TIME) {
#ifdef LUX_METER
		// Read data from BH1750
		Wire.requestFrom(LUX_METER_ADDRESS, (uint8_t)2);
		lux_raw_data = Wire.read();
		lux_raw_data = (lux_raw_data * (uint16_t)256) + Wire.read();
		converted_lux = (float)lux_raw_data / 0.54;
#else
		// Read data from LDR sensor
		// Convert the raw digital data back to the voltage that was measured on the analog pin
		resistor_voltage = (float)analogRead(LDR_PIN) / 1023.0 * ADC_REF_VOLTAGE;

		// Voltage across the LDR is the 5V supply minus the second resistor voltage
		ldr_voltage = ADC_REF_VOLTAGE - resistor_voltage;

		// Resistance that the LDR would have for that voltage
		ldr_resistance = ldr_voltage / resistor_voltage * REF_RESISTANCE;

		// Change the code below to the proper conversion from ldrResistance to LUX
		converted_lux = LUX_CALC_SCALAR * pow(ldr_resistance, LUX_CALC_EXPONENT);

		// Convert value to raw 16 bit value
		lux_raw_data = converted_lux * 0.54;

#endif
		// Convert to sinle byte (just find sqrt)
		lux_raw_data = sqrt(lux_raw_data);
		if (lux_raw_data > 255)
			lux_raw_data = 255;
		lux_sqrt_data = lux_raw_data;

		// Reset timer
		lux_timer = millis();
	}
}
