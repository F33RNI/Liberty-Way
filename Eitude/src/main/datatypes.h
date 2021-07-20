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

#ifndef DATATYPES_H
#define DATATYPES_H

// Common variables
uint8_t error;
uint16_t count_var;
uint32_t loop_timer;

// LCD
uint32_t lcd_timer;

// Serial
char buffer[BUFFER_SIZE];
int16_t sofar;
int16_t command;

// Light sensor
uint16_t raw_illumination;
float filtered_value;

// MPU6050
int16_t temperature;
int16_t acc_x, acc_y, acc_z;
int16_t gyro_pitch, gyro_roll, gyro_yaw;
uint8_t level_calibration_on;
int32_t gyro_pitch_cal, gyro_roll_cal, gyro_yaw_cal;
int32_t acc_x_cal_value, acc_y_cal_value;
int32_t acc_x_filtered, acc_y_filtered;

// Vibration meter
int32_t vibration_array[20], avarage_vibration_level, vibration_total_result;
uint8_t vibration_counter, in_move_flag;

// Speed
float speed, speed_accumulator;
int32_t speed_loop_timer;

#endif
