/*
 * Copyright (C) 2021 Fern H. (aka Pavel Neshumov), Liberty Drones GPS mixer
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

// TODO: Optimize data transfer

// System config, constants and variables
#include "config.h"
#include "constants.h"
#include "datatypes.h"

// Define functions with pointers
void gps_read(HardwareSerial* gps_serial, serial_container* serial_container_, gps_container* gps_container_r);
void nmea_read(serial_container* serial_container_, gps_container* gps_container_);
void nmea_parse(serial_container* serial_container_, gps_container* gps_container_);
void append_to_sum(gps_container* container);
void reset_gps_container(gps_container* container);

void setup()
{
    // Store packet ending
    output_buffer[18] = PACKET_SUFFIX_1;
    output_buffer[19] = PACKET_SUFFIX_2;

    // Enable LED
    led_setup();

    // Open gps tx serial port at default baud rate
    GPS_TX_SERIAL.begin(9600);
    delay(250);

    // Disable all default messages and enable GGA and VTG messages
    for (gps_cfg_id = 0; gps_cfg_id <= 10; gps_cfg_id++) {
        // Set message ID
        GPS_CFG_BUFFER[7] = gps_cfg_id;

        // Enable or disable NMEA message
        if (gps_cfg_id == 0x00 || gps_cfg_id == 0x05)
            GPS_CFG_BUFFER[8] = 0x01;
        else
            GPS_CFG_BUFFER[8] = 0x00;

        // Reset check-sums
        GPS_CFG_BUFFER[9] = 0;
        GPS_CFG_BUFFER[10] = 0;

        // Calculate check-sums
        for (count_var = 2; count_var <= 8; count_var++) {
            GPS_CFG_BUFFER[9] = (GPS_CFG_BUFFER[9] + GPS_CFG_BUFFER[count_var]) & 0xFF;
            GPS_CFG_BUFFER[10] = (GPS_CFG_BUFFER[9] + GPS_CFG_BUFFER[10]) & 0xFF;
        }

        // Send command
        GPS_TX_SERIAL.write(GPS_CFG_BUFFER, 11);
        delay(100);
    }

    // Set the refresh rate to 10Hz
    GPS_TX_SERIAL.write(GPS_SET_TO_10HZ, 14);
    delay(350);

    // Set the baud rate
    GPS_TX_SERIAL.write(GPS_SET_BAUD_RATE, 28);
    delay(200);

    // Open gps tx serial port at new bad rate
    GPS_TX_SERIAL.begin(SERIAL_BAUD_RATE);
    delay(200);

    // Open input serial ports
#ifdef GPS_1_RX_SERIAL
    GPS_1_RX_SERIAL.begin(SERIAL_BAUD_RATE);
    delay(200);
#endif
#ifdef GPS_2_RX_SERIAL
    GPS_2_RX_SERIAL.begin(SERIAL_BAUD_RATE);
    delay(200);
#endif
#ifdef GPS_3_RX_SERIAL
    GPS_3_RX_SERIAL.begin(SERIAL_BAUD_RATE);
    delay(200);
#endif

    // Open host serial port
    OUTPUT_SERIAL.begin(SERIAL_BAUD_RATE);
    delay(200);
}

void loop()
{
    // Send serial data withput interrupts
    while (new_data_flag)
        data_sender();

    // Read data from serial ports
#ifdef GPS_1_RX_SERIAL
    gps_read(&GPS_1_RX_SERIAL, &serial_container_1, &gps_container_1);
#endif
#ifdef GPS_2_RX_SERIAL
    gps_read(&GPS_2_RX_SERIAL, &serial_container_2, &gps_container_2);
#endif
#ifdef GPS_3_RX_SERIAL
    gps_read(&GPS_3_RX_SERIAL, &serial_container_3, &gps_container_3);
#endif

    // If at least one receiver is available
    if (working_receivers_num && !timeout_started_flag) {
        timeout_started_flag = 1;
        timeout_timer = millis();
    }

    if (timeout_started_flag
        && (working_receivers_num >= CONNECTED_RECEIVERS || millis() - timeout_timer >= TIMEOUT_TIME)) {
        timeout_started_flag = 0;

        // Handle new data
        combine_new_data();

        // Set the output flag, reset counter and lost timer
        new_data_flag = 1;
        output_buffer_position = 0;
        lost_timer = millis();

        // Reset containers
#ifdef GPS_1_RX_SERIAL
        reset_gps_container(&gps_container_1);
        reset_serial_container(&serial_container_1);
#endif
#ifdef GPS_2_RX_SERIAL
        reset_gps_container(&gps_container_2);
        reset_serial_container(&serial_container_2);
#endif
#ifdef GPS_3_RX_SERIAL
        reset_gps_container(&gps_container_3);
        reset_serial_container(&serial_container_3);
#endif

        // Reset working_receivers number
        working_receivers_num_last = working_receivers_num;
        working_receivers_num = 0;
    }

    // Check if GPS is lost
    if (millis() - lost_timer > GPS_LOST_TIME)
        working_receivers_num_last = 0;

    // Send output message
    data_sender();

    // Show number of working receivers with LED
    led_handler();
}

/// <summary>
/// Map function for double type
/// </summary>
double map_double(double x, double in_min, double in_max, double out_min, double out_max) {
    return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
}
