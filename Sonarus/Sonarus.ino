/*
 * Copyright (C) 2021 Fern Hertz (Pavel Neshumov), Sonarus - I2C ultrasonic rangefinder
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

// I2C library
#include <Wire.h>

// System variables
boolean power_state, power_mode, resolution;
boolean start_flag;
float filter;
uint64_t sonar_1_timer, sonar_2_timer;
uint64_t sonar_1_duration, sonar_2_duration;
float distance_1_raw, distance_2_raw, distance_1, distance_2;
uint8_t s_distance_1, s_distance_2;
uint16_t l_distance_1, l_distance_2;
boolean sonar_1_timeout, sonar_2_timeout;
float sonar_1_correction, sonar_2_correction;
uint8_t i2c_command;
uint16_t i2c_sound_speed;

// Speed of sound in m/s divided by 10000
float sound_speed = 0.03431;

// I2C address of sink
const uint8_t I2C_ADDRESS PROGMEM = 0xEE;

// Sonar 1 hardware pins
#define SONAR_1_TRIG_PORT   PORTD
const uint8_t SONAR_1_TRIG_PIN PROGMEM = B00010000;
const uint8_t SONAR_1_TRIG_ARD_PIN PROGMEM = 4;
const uint8_t SONAR_1_ECHO_ARD_PIN PROGMEM = 2;

// Sonar 2 hardware pins
#define SONAR_2_TRIG_PORT   PORTD
const uint8_t SONAR_2_TRIG_PIN PROGMEM = B00100000;
const uint8_t SONAR_2_TRIG_ARD_PIN PROGMEM = 5;
const uint8_t SONAR_2_ECHO_ARD_PIN PROGMEM = 3;

// Maximum time to wait for a response from sonar. MIN: ((255 * 2) / SOUND_SPEED * 2)
const uint16_t TIMEOUT_US PROGMEM = 32000;

void setup()
{
    // Setup I2C as sink
    Wire.begin(I2C_ADDRESS);
    delay(100);

    // Setup hardware pins
    pinMode(SONAR_1_TRIG_ARD_PIN, OUTPUT);
    pinMode(SONAR_1_ECHO_ARD_PIN, INPUT);
    pinMode(SONAR_2_TRIG_ARD_PIN, OUTPUT);
    pinMode(SONAR_2_ECHO_ARD_PIN, INPUT);
    pinMode(LED_BUILTIN, OUTPUT);

    // Connect interrupts
    attachInterrupt(digitalPinToInterrupt(SONAR_1_ECHO_ARD_PIN), sonar_1_event, FALLING);
    attachInterrupt(digitalPinToInterrupt(SONAR_2_ECHO_ARD_PIN), sonar_2_event, FALLING);

    // Connect I2C callbacks
    Wire.onReceive(on_receive_event);
    Wire.onRequest(on_request_event);
}

void loop()
{
    // Request new distance
    if (start_flag) {
        sonarus();
        start_flag = 0;
    }
}

/// <summary>
/// Function that executes whenever data is received from source
/// </summary>
void on_request_event(void) {
    if (resolution) {
        // Send distances in high resolution mode (2 byte per sonar)
        Wire.write(l_distance_1 >> 8);
        Wire.write(l_distance_1);
        Wire.write(l_distance_2 >> 8);
        Wire.write(l_distance_2);
    }
    else {
        // Send distances in low resolution mode (1 byte per sonar)
        Wire.write(s_distance_1);
        Wire.write(s_distance_2);
    }

    // Begin new reading if in continuously mode
    if (power_mode)
        start_flag = 1;
}

/// <summary>
/// Function that executes whenever data is requested by source
/// </summary>
void on_receive_event(int amount) {
    // Read command from I2C
    i2c_command = Wire.read();

    // Power off
    if (i2c_command == 0x00)
        power_state = 0;

    // Power on
    else if (i2c_command == 0x01)
        power_state = 1;

    // Select measurment mode (0x00 - single / 0x01 - continuously)
    else if (i2c_command == 0x02)
        power_mode = Wire.read();

    // Select output resolution (0x00 - 1 byte (result divided by 2) / 0x01 - 2 bytes (result multiplied by 100))
    else if (i2c_command == 0x03)
        resolution = Wire.read();

    // Set factor of the filter
    else if (i2c_command == 0x04)
        filter = (float)Wire.read() / 100.0;

    // Set sound speed
    else if (i2c_command == 0x05) {
        // Set from 2 bytes
        i2c_sound_speed = Wire.read() << 8 | Wire.read();

        // Convert to float
        sound_speed = (float)i2c_sound_speed / 10000.0;
    }

    // Set distance corrections
    else if (i2c_command == 0x06) {
        // Set first sonar corrections from 2 bytes
        sonar_1_correction = (int16_t)(Wire.read() << 8 | Wire.read()) / 10.0;

        // Set second sonar corrections from 2 bytes
        sonar_2_correction = (int16_t)(Wire.read() << 8 | Wire.read()) / 10.0;
    }

    // Request new measurment (single mode)
    else if (i2c_command == 0x07)
        start_flag = 1;
}

/// <summary>
/// Masures distance using both sonars
/// </summary>
void sonarus(void) {
    // Check if powered on
    if (power_state) {
        // Measure distances
        delay(2);
        sonar_1();
        delay(2);
        sonar_2();
    }
}

/// <summary>
/// Measures distance using first sonar. The maximum void time is TIMEOUT_US
/// </summary>
void sonar_1(void) {
    // Reset variables
    sonar_1_timeout = 0;
    sonar_1_duration = 0;

    // Turn on builtin led
    digitalWrite(LED_BUILTIN, 1);

    // Send 10us pulse for tx burst
    SONAR_1_TRIG_PORT = SONAR_1_TRIG_PIN;
    delayMicroseconds(10);
    SONAR_1_TRIG_PORT = 0;

    // Save the end time of the tx signal
    sonar_1_timer = micros();

    // Turn off builtin led
    digitalWrite(LED_BUILTIN, 0);

    // Wait for the end of the measurement
    while (!sonar_1_duration) {
        if (micros() - sonar_1_timer > TIMEOUT_US) {
            sonar_1_timeout = 1;
            break;
        }
    }

    // Calculate distance in cm
    if (sonar_1_timeout)
        distance_1_raw = 0.0;
    else {
        distance_1_raw = (float)sonar_1_duration * sound_speed / 2.0 + sonar_1_correction;
        if (distance_1_raw > 510.0 || distance_1_raw < 0.0)
            distance_1_raw = 0.0;
    }

    // Filter distance
    if (distance_1_raw == 0.0)
        distance_1 = 0.0;
    else
        distance_1 = distance_1 * filter + (1.0 - filter) * distance_1_raw;

    // Convert float to other types
    s_distance_1 = distance_1 / 2.0;
    l_distance_1 = distance_1 * 100.0;
}

/// <summary>
/// Measures distance using second sonar. The maximum void time is TIMEOUT_US
/// </summary>
void sonar_2(void) {
    // Reset variables
    sonar_2_timeout = 0;
    sonar_2_duration = 0;

    // Turn on builtin led
    digitalWrite(LED_BUILTIN, 1);

    // Send 10us pulse for tx burst
    SONAR_2_TRIG_PORT = SONAR_2_TRIG_PIN;
    delayMicroseconds(10);
    SONAR_2_TRIG_PORT = 0;

    // Save the end time of the tx signal
    sonar_2_timer = micros();

    // Turn off builtin led
    digitalWrite(LED_BUILTIN, 0);

    // Wait for the end of the measurement
    while (!sonar_2_duration) {
        if (micros() - sonar_2_timer > TIMEOUT_US) {
            sonar_2_timeout = 2;
            break;
        }
    }

    // Calculate distance in cm
    if (sonar_2_timeout)
        distance_2_raw = 0.0;
    else {
        distance_2_raw = (float)sonar_2_duration * sound_speed / 2.0 + sonar_2_correction;
        if (distance_2_raw > 510.0 || distance_2_raw < 0.0)
            distance_2_raw = 0.0;
    }

    // Filter distance
    if (distance_2_raw == 0.0)
        distance_2 = 0.0;
    else
        distance_2 = distance_2 * filter + (1.0 - filter) * distance_2_raw;

    // Convert float to other types
    s_distance_2 = distance_2 / 2.0;
    l_distance_2 = distance_2 * 100.0;
}

/// <summary>
/// First sonar interrupt callback
/// </summary>
void sonar_1_event(void) {
    // Measure the time difference between the end of the tx signal and the falling edge
    sonar_1_duration = micros() - sonar_1_timer;
}

/// <summary>
/// Second sonar interrupt callback
/// </summary>
void sonar_2_event(void) {
    // Measure the time difference between the end of the tx signal and the falling edge
    sonar_2_duration = micros() - sonar_2_timer;
}
