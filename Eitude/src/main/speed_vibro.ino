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
/// Calculates speed of the platform
/// </summary>
void speed_handler(void) {
	// Filter accelerations
	acc_x_filtered = (float)acc_x_filtered * ACC_FILTER_KOEFF + (float)acc_x * (1.0 - ACC_FILTER_KOEFF);
	acc_y_filtered = (float)acc_y_filtered * ACC_FILTER_KOEFF + (float)acc_y * (1.0 - ACC_FILTER_KOEFF);

	speed = acc_x_filtered;

	// Convert acceleration to G
	speed /= 4096.0;
	// Convert to m/s^2
	speed *= 9.81;	
	// Multiply by dt to get instant speed in m/ms
	speed *= (millis() - speed_loop_timer);

	// Reset timer
	speed_loop_timer = millis();

	// Convert to m/s
	speed /= 1000.0;						
	// Convert to km/h
	speed *= 3.6;						
	
	// Accumulate instatnt speed
	speed_accumulator += speed;

	if (!in_move_flag) {
		// If the platform is not moving, reset the speed
		speed_accumulator = speed_accumulator * SPEED_ZEROING_FACTOR;
	}
}

/// <summary>
/// Checks vibration level and sets in_move flag
/// </summary>
void vibrations(void) {
	// Calculate the total accelerometer vector.
	vibration_array[0] = (int32_t)acc_x * (int32_t)acc_x;
	vibration_array[0] += (int32_t)acc_y * (int32_t)acc_y;
	vibration_array[0] += (int32_t)acc_z * (int32_t)acc_z;
	vibration_array[0] = sqrt(vibration_array[0]);

	for (count_var = 16; count_var > 0; count_var--) {
		// Shift every variable one position up in the array.
		vibration_array[count_var] = vibration_array[count_var - 1];
		// Add the array value to the acc_av_vector variable.
		avarage_vibration_level += vibration_array[count_var];
	}
	// Divide the acc_av_vector by 17 to get the avarage total accelerometer vector.
	avarage_vibration_level /= 17;

	if (vibration_counter < 20) {
		vibration_counter++;
		// Add the absolute difference between the avarage vector and current vector to the vibration_total_result variable.
		vibration_total_result += abs(vibration_array[0] - avarage_vibration_level);
		
	}
	else {
		in_move_flag = vibration_total_result > VIBR_STOP_MOVE_THRESH;
		vibration_counter = 0;
		// Serial.println(vibration_total_result);
		vibration_total_result = 0;
	}
}