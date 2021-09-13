/*
 * Copyright (C) 2021 Fern H. (Pavel Neshumov), Liberty Drones GPS mixer
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

/// <summary>
/// Setups LED pin as output
/// </summary>
/// <param name=""></param>
void led_setup(void) {
	pinMode(LED_BUILTIN, OUTPUT);
	digitalWrite(LED_BUILTIN, 1);
}

/// <summary>
/// Flashes the LED as many times as the receivers are working
/// </summary>
/// <param name=""></param>
void led_handler(void) {
	if (millis() - led_timer >= LED_CYCLE_TIME) {
		// Reset timer
		led_timer = millis();

		// Reset led_receivers_counter after +3 cycles (for delay)
		if (working_receivers_num_last && led_receivers_counter > working_receivers_num_last + 3)
			led_receivers_counter = 0;
		if (working_receivers_num_last && led_receivers_counter < working_receivers_num_last && !led_state) {
			// Turn LED on
			set_led_state(1);
		}
		else {
			// Turn LED off
			set_led_state(0);

			// Increment counter
			led_receivers_counter++;
		}
	}
}

/// <summary>
/// Sets LED state
/// </summary>
/// <param name="state"></param>
void set_led_state(boolean state) {
	digitalWrite(LED_BUILTIN, !state);
	led_state = state;
}
