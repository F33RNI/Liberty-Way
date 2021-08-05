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

#ifdef BAROMETER

 /// <summary>
 /// Initializes MS5611 barometer
 /// </summary>
void barometer_setup(void) {
    // Check if the barometer is responding
    Wire.beginTransmission(BAROMETER_ADDRESS);
    error = Wire.endTransmission();
    while (error != 0) {
        // Stay in the loop because the barometer did not responde
        error = 3;

        // Show curent error
        leds_error_signal();

        // Simulate main loop
        delayMicroseconds(LOOP_PERIOD);
    }
       
    // Restart barometer
    Wire.beginTransmission(BAROMETER_ADDRESS);
    Wire.write(0x1E);
    Wire.endTransmission();

    // Waiting until MS5611 restarts
    delay(100);

    // Extract 6 calibration values from the memory location 0xA2 and up
    for (count_var = 1; count_var <= 6; count_var++) {
        // Request 2 bytes from the MS5611
        Wire.beginTransmission(BAROMETER_ADDRESS);
        Wire.write(0xA0 + (count_var * 2));
        Wire.endTransmission();
        Wire.requestFrom(BAROMETER_ADDRESS, (uint8_t)2);

        // Add the low and high byte to the C[x] calibration variable
        C[count_var] = Wire.read();
        C[count_var] = (C[count_var] * (uint16_t)256) + Wire.read();
    }

    // This values is pre-calculated to offload the main program loop
    OFF_C2 = C[2] * (int64_t)65536;
    SENS_C1 = C[1] * (int64_t)32768;

    // Stabilize pressure with a few readings
    for (count_var = 0; count_var < 500; count_var++) {
        // Read barometer data
        barometer_handler();

        // Blink with LEDs
        if (count_var % 50 == 0)
            leds_calibration_signal();

        // Simulate main loop
        delayMicroseconds(LOOP_PERIOD);
    }

    // Align the pressure (fast start)
    actual_pressure_slow = actual_pressure_fast;
    actual_pressure = actual_pressure_fast + PRESSURE_CORRECTION;

    // Stabilize pressure again with a few readings
    for (count_var = 0; count_var < 100; count_var++) {
        // Read barometer data
        barometer_handler();

        // Blink with LEDs
        if (count_var % 50 == 0)
            leds_calibration_signal();

        // Simulate main loop
        delayMicroseconds(LOOP_PERIOD);
    }
}

/// <summary>
/// Reads data from the barometer
/// </summary>
void barometer_handler(void) {
    // Every time this function is called the barometer_counter variable is incremented 
    // This is needed because requesting data from the barometer takes around 9ms to complete
    barometer_counter++;

    if (barometer_counter == 1) {
        // Step 1. Read raw values
        if (temperature_counter == 0) {
            // Store the temperature in a 5 location rotating memory to prevent temperature spikes.
            raw_average_temperature_total -= raw_temperature_rotating_memory[average_temperature_mem_location];

            // Get temperature data from barometer
            Wire.beginTransmission(BAROMETER_ADDRESS);
            Wire.write(0x00);
            Wire.endTransmission();
            Wire.requestFrom(BAROMETER_ADDRESS, (uint8_t)3);

            // Poll 3 data bytes from the MS5611
            raw_temperature_rotating_memory[average_temperature_mem_location] = Wire.read();
            raw_temperature_rotating_memory[average_temperature_mem_location] = 
                (raw_temperature_rotating_memory[average_temperature_mem_location] * (uint32_t)256) + Wire.read();
            raw_temperature_rotating_memory[average_temperature_mem_location] = 
                (raw_temperature_rotating_memory[average_temperature_mem_location] * (uint32_t)256) + Wire.read();

            raw_average_temperature_total += raw_temperature_rotating_memory[average_temperature_mem_location];
            average_temperature_mem_location++;
            if (average_temperature_mem_location == 5)
                average_temperature_mem_location = 0;
            // Calculate the avarage temperature of the last 5 measurements
            raw_temperature = raw_average_temperature_total / 5;
        }
        else {
            // Get pressure data from MS-5611
            Wire.beginTransmission(BAROMETER_ADDRESS);
            Wire.write(0x00);
            Wire.endTransmission();
            Wire.requestFrom(BAROMETER_ADDRESS, (uint8_t)3);

            // Poll 3 data bytes from the MS5611
            raw_pressure = Wire.read();
            raw_pressure = (raw_pressure * (uint32_t)256) + Wire.read();
            raw_pressure = (raw_pressure * (uint32_t)256) + Wire.read();
        }

        temperature_counter++;
        if (temperature_counter == 20) {
            // Reset the temperature_counter when the temperature counter equals 20
            temperature_counter = 0;
            // Request temperature data
            Wire.beginTransmission(BAROMETER_ADDRESS);
            Wire.write(0x58);
            Wire.endTransmission();
        }
        else {
            //Request pressure data
            Wire.beginTransmission(BAROMETER_ADDRESS);
            Wire.write(0x48);
            Wire.endTransmission();
        }
    }
    if (barometer_counter == 2) {
        // Step 2. Calculate pressure as explained in the datasheet of the MS-5611
        dT = C[5];
        dT <<= 8;
        dT *= -1;
        dT += raw_temperature;
        OFF = OFF_C2 + ((int64_t)dT * (int64_t)C[4]) / 128LL;
        SENS = SENS_C1 + ((int64_t)dT * (int64_t)C[3]) / 256LL;
        P = ((raw_pressure * SENS) / 2097152UL - OFF) / 32768UL; 2000L + dT * C[6] / 8388608L;

        // 20 location rotating memory to get a smoother pressure value
        // Subtract the current memory position to make room for the new value
        pressure_total_avarage -= pressure_rotating_mem[pressure_rotating_mem_location];
        // Calculate the new change between the actual pressure and the previous measurement
        pressure_rotating_mem[pressure_rotating_mem_location] = P;
        // Add the new value to the long term avarage value
        pressure_total_avarage += pressure_rotating_mem[pressure_rotating_mem_location];
        // Increase the rotating memory location
        pressure_rotating_mem_location++;
        // Start at 0 when the memory location 20 is reached
        if (pressure_rotating_mem_location == 20)
            pressure_rotating_mem_location = 0;
        // Calculate the average pressure of the last 20 pressure readings
        actual_pressure_fast = (float)pressure_total_avarage / 20.0;

        // Complementary fillter that can be adjusted by the fast average to get better results
        actual_pressure_slow = actual_pressure_slow * (float)0.985 + actual_pressure_fast * (float)0.015;
        // Calculate the difference between the fast and the slow avarage value
        actual_pressure_diff = actual_pressure_slow - actual_pressure_fast;
        // If the difference is larger then 8 limit the difference to 8
        if (actual_pressure_diff > 8)actual_pressure_diff = 8;
        // If the difference is smaller then -8 limit the difference to -8
        if (actual_pressure_diff < -8)actual_pressure_diff = -8;
        // If the difference is larger then 1 or smaller then -1 the slow average is adjuste based on the error between the fast and slow average.
        if (actual_pressure_diff > 1 || actual_pressure_diff < -1)actual_pressure_slow -= actual_pressure_diff / 6.0;
        // The actual_pressure is used in the program for altitude calculations.
        actual_pressure = actual_pressure_slow + PRESSURE_CORRECTION;
    }

    if (barometer_counter == 3) {
        // Step 3. Reset counter
        barometer_counter = 0;
    }
}
#endif
