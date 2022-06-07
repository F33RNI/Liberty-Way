/**
 * Copyright (C) 2022 Vladislav Yasnetsky, Eitude AMLS Platform controller
 * This software is part of Liberty Drones Project
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

#include "datatypes.h"
#include "config.h"
#include "lux_meter.h"

/**
 * @brief Initializes BH1750
 * 
 */
void lux_meter_setup(void) {
    // Start communication with BH1750
    Wire.beginTransmission(LUX_METER_ADDRESS);

    // Power on BH1750
    Wire.write(0x01);

    // Check BH1750
    error = Wire.endTransmission();
    if (error != 0) {
        // Lux meter did not response
        error = ERROR_LUX_METER;
        while (error != 0)
            // Show current error
            status_led();
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

/**
 * @brief Retrieves data from BH1750 and converts to single-byte value
 * 
 */
void lux_meter(void) {
    // Request data from lux meter every LUX_REQUEST_TIME ms
    if (millis() - lux_timer >= LUX_REQUEST_TIME) {
        // Request 2 bytes from sensor
        Wire.requestFrom(LUX_METER_ADDRESS, 2);

        // Read 2 bytes from sensor
        lux_raw_data = Wire.read() << 8 | Wire.read();

        // Convert to lux (divide by 0.54)
        lux_data = lux_raw_data / 0.54;

        // Compress value
        lux_data = pow(lux_data, 0.475);
        if (lux_data > 254.0)
            lux_data = 254.0;

        // Convert to sinle byte
        lux_sqrt_data = lux_data;

        // Reset timer
        lux_timer = millis();
    }
}