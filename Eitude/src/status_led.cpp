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
#include "status_led.h"

/**
 * @brief Initialized status (error) LED
 * 
 */
void status_led_setup(void) {
    // Set pin as output
    pinMode(PIN_STATUS_LED, OUTPUT);
}

/**
 * @brief Shows current error with the LED
 * 
 */
void status_led(void) {
    if (millis() - led_timer >= LED_CYCLE_TIME) {
        // Reset timer
        led_timer = millis();

        // Reset led_cycle_counter after +3 cycles (for delay)
        if (error && led_cycle_counter > error + 3)
            led_cycle_counter = 0;

        if (error && led_cycle_counter < error && !led_state) {
            // Turn LED on
            digitalWrite(PIN_STATUS_LED, 1);
            led_state = 1;
        }
        else {
            // Turn LED off
            digitalWrite(PIN_STATUS_LED, 0);
            led_state = 0;

            // Increment counter
            led_cycle_counter++;
        }
    }
}