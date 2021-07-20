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
/// Initializes MPU-6050 IMU
/// </summary>
void imu_setup(void) {
	// Check if the IMU is responding
	Wire.beginTransmission(IMU_ADDRESS);
	error = Wire.endTransmission();
	while (error != 0) {
		// Stay in the loop because the IMU did not responde
		error = 1;
		// Show cuurent error
		lcd_print_error();
		// Simulate main loop
		delayMicroseconds(LOOP_PERIOD);
	}

	Wire.beginTransmission(IMU_ADDRESS);
	// Set the PWR_MGMT_1 register (6B hex) bits as 00000000 to activate the gyro.
	Wire.write(0x6B);
	Wire.write(0x00);
	Wire.endTransmission();

	Wire.beginTransmission(IMU_ADDRESS);
	// Set the GYRO_CONFIG register (1B hex) bits as 00001000 (500dps full scale)
	Wire.write(0x1B);
	Wire.write(0x08);
	Wire.endTransmission();

	Wire.beginTransmission(IMU_ADDRESS);
	// Set the  ACCEL_CONFIG register (1A hex) bits as 00010000 (+/- 8g full scale range)
	Wire.write(0x1C);
	Wire.write(0x10);
	Wire.endTransmission();

	Wire.beginTransmission(IMU_ADDRESS);
	// Set the CONFIG register (1A hex) bits as 00000011 (Set Digital Low Pass Filter to ~43Hz)
	Wire.write(0x1A);
	Wire.write(0x03);
	Wire.endTransmission();
}

/// <summary>
/// Reads raw data from the IMU with calibrartions
/// </summary>
void imu_read(void) {
	Wire.beginTransmission(IMU_ADDRESS);
	// Start reading @ register 43h and auto increment with every read
	Wire.write(0x3B);
	Wire.endTransmission();

	// Request 14 bytes from the MPU 6050.
	Wire.requestFrom(IMU_ADDRESS, (uint8_t)14);

	// Add the low and high byte to the acc variables
	acc_x = Wire.read() << 8 | Wire.read();
	acc_y = Wire.read() << 8 | Wire.read();
	acc_z = Wire.read() << 8 | Wire.read();

	// Add the low and high byte to the temperature variable
	temperature = Wire.read() << 8 | Wire.read();

	// Read high and low parts of the angular data
	gyro_roll = Wire.read() << 8 | Wire.read();
	gyro_pitch = Wire.read() << 8 | Wire.read();
	gyro_yaw = Wire.read() << 8 | Wire.read();

	// Invert the direction of the axes
	//gyro_roll *= -1;
	gyro_pitch *= -1;
	gyro_yaw *= -1;

	if (level_calibration_on == 0) {
		// Subtact the calibration values
		acc_x -= acc_x_cal_value;
		acc_y -= acc_y_cal_value;
		gyro_roll -= gyro_roll_cal;
		gyro_pitch -= gyro_pitch_cal;
		gyro_yaw -= gyro_yaw_cal;
	}
}

/// <summary>
/// Calibartes gyro and acc
/// </summary>
void imu_calibrate(void) {
	// Disable subtracting calibration values
	level_calibration_on = 1;
	lcd.clear();
	lcd.setCursor(0, 0);
	lcd.print(F("  Calibration   "));
	lcd.setCursor(0, 1);

	acc_x_cal_value = 0;
	acc_y_cal_value = 0;
	gyro_roll_cal = 0;
	gyro_pitch_cal = 0;
	gyro_yaw_cal = 0;
	for (count_var = 0; count_var < CALIBRATION_N; count_var++) {
		imu_read();
		acc_x_cal_value += acc_x;
		acc_y_cal_value += acc_y;
		gyro_roll_cal += gyro_roll;
		gyro_pitch_cal += gyro_pitch;
		gyro_yaw_cal += gyro_yaw;

		if (count_var % (CALIBRATION_N / 16) == 0)
			lcd.print('.');
		delayMicroseconds(4000);
	}
	acc_x_cal_value /= CALIBRATION_N;
	acc_y_cal_value /= CALIBRATION_N;
	gyro_roll_cal /= CALIBRATION_N;
	gyro_pitch_cal /= CALIBRATION_N;
	gyro_yaw_cal /= CALIBRATION_N;

	// Enable subtracting calibration values (calibration done)
	level_calibration_on = 0;
	lcd.clear();

	imu_read();
	speed_loop_timer = millis();
}