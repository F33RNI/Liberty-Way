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

// System config, libraries and variables
#include "datatypes.h"
#include "config.h"

volatile uint8_t error;
volatile uint8_t system_status;
volatile uint8_t backlight_state;
volatile uint8_t alignment_state;
volatile int32_t l_lat_gps, l_lon_gps;
volatile uint8_t number_used_sats;
volatile uint8_t hdop;
volatile int16_t altitude;
volatile uint16_t ground_heading;
volatile uint16_t ground_speed;
volatile uint8_t lux_sqrt_data;

void setup() {
  // Initialize I2C
  Wire.begin();

  // Setup status (error) LED
  status_led_setup();

  // Setup stepper motors
  motors_setup();

  // Setup backlight
  backlight_setup();

  // Setup LUX-meter
  lux_meter_setup();

  // Go to home position
  motors_home();

  // Setup ethernet communication
  communication_setup();

  // Setup GPS-Mixer
  gps_reader_setup();
}

void loop() {
  // Ethernet loop cycle
  communication_ether_packet_loop();

  // Receive data from GPS-Mixer
  gps_reader();

  // Measure ambient illumination in LUX
  lux_meter();

  // Turn on or off backlight
  backlight();

  // Alignment system loop cycle
  motors_alignment_system();

  // Send tx_buffer
  data_send();

  // Error LED loop cycle
  status_led();
}