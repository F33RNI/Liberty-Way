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
#ifdef LUX_METER
	// Request LUX data by timer
	if (millis() - lux_cycle_timer >= LUX_REQUST_TIME) {
		// Restart timer
		lux_cycle_timer = millis();

		// Read data from BH1750
		Wire.requestFrom(LUX_METER_ADDRESS, (uint8_t)2);
		lux_raw_data = Wire.read();
		lux_raw_data = (lux_raw_data * (uint16_t)256) + Wire.read();

		// Convert to sinle byte (just find sqrt)
		converted_lux = pow((float)lux_raw_data / 0.54f, 0.475);
		if (converted_lux > 255.0)
			converted_lux = 255.0;
		lux_sqrt_data = converted_lux;
	}
#else
	// Read data from LDR sensor
	// Convert the raw digital data back to the voltage that was measured on the analog pin
	resistor_divider_voltage = (float)analogRead(LDR_OUT_PIN) / 1023.f * ADC_REF_VOLTAGE;


	// The LDR module is a resistor voltage divider.
	// The upper resistor is known and connected to VCC, the lower resistor is LDR and connected to ground.
	// 
	// ADC_REF_VOLTAGE
	// |
	// Rupper (REF_RESISTANCE)
	// |
	// -- resistor_divider_voltage
	// |
	// Rlower (LDR)
	// |
	// GND
	//
	// So, the formula for resistor voltage divider is:
	// Uupper = ADC_REF_VOLTAGE * (REF_RESISTANCE / (REF_RESISTANCE + Rlower))
	//
	// Where Uupper is (ADC_REF_VOLTAGE - resistor_divider_voltage)
	// So, the final formula is
	//
	// Rlower = ((ADC_REF_VOLTAGE * REF_RESISTANCE) / (ADC_REF_VOLTAGE - resistor_divider_voltage)) - REF_RESISTANCE

	// Resistance of the LDR
	ldr_resistance = ((ADC_REF_VOLTAGE * REF_RESISTANCE) / (ADC_REF_VOLTAGE - resistor_divider_voltage)) - REF_RESISTANCE;

	// Convert resistance to LUX
	unfiltered_lux = 12500000.f * pow(ldr_resistance, -1.4059);

	// Filter it
	if (millis() < 3000)
		filtered_lux = unfiltered_lux;
	else
		filtered_lux = filtered_lux * 0.95f + unfiltered_lux * 0.05f;

	// Convert to sinle byte (just find sqrt)
	converted_lux = pow(filtered_lux, 0.475);
	if (converted_lux > 255)
		converted_lux = 255;
	lux_sqrt_data = converted_lux;
#endif	
}
