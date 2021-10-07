/*
 * Copyright (C) 2021 Fern H. (aka Pavel Neshumov), Eitude AMLS Platform controller
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
 *
 * IT IS STRICTLY PROHIBITED TO USE THE PROJECT (OR PARTS OF THE PROJECT / CODE)
 * FOR MILITARY PURPOSES. ALSO, IT IS STRICTLY PROHIBITED TO USE THE PROJECT (OR PARTS OF THE PROJECT / CODE)
 * FOR ANY PURPOSE THAT MAY LEAD TO INJURY, HUMAN, ANIMAL OR ENVIRONMENTAL DAMAGE.
 * ALSO, IT IS PROHIBITED TO USE THE PROJECT (OR PARTS OF THE PROJECT / CODE) FOR ANY PURPOSE THAT
 * VIOLATES INTERNATIONAL HUMAN RIGHTS OR HUMAN FREEDOM.
 * BY USING THE PROJECT (OR PART OF THE PROJECT / CODE) YOU AGREE TO ALL OF THE ABOVE RULES.
 */

 /*
  * ATTENTION! THIS IS A BETA VERSION OF EITUDE. INSTEAD OF GPS-MIXER,
  * WE USE GETTING GPS-COORDINATES FROM THE PHONE USING THE GPS-TO-SERIAL APP
  * MORE INFO AT: https://github.com/XxOinvizioNxX/GPS-to-Serial
  *
 */

/// <summary>
/// Enables or disables backlight according to backlight_state
/// </summary>
void backlight(void) {
	if (backlight_state && !digitalRead(LIGHTS_PIN))
		// Enable backlight
		digitalWrite(LIGHTS_PIN, 1);
	else if (!backlight_state && digitalRead(LIGHTS_PIN))
		// Disable backlight
		digitalWrite(LIGHTS_PIN, 0);
}
