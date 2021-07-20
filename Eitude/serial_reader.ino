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
/// Fills buffer with the received data
/// </summary>
void serial_reader(void) {
	while (Serial.available() > 0) {
		char c = Serial.read();
		if (sofar < BUFFER_SIZE - 1) {
			buffer[sofar++] = c;
		}
		else {
			sofar = 0;
			Serial.flush();
		}
		if ((c == '\n') || (c == '\r')) {
			buffer[sofar] = 0;

			// Check if buffer is empty
			if (sofar < 2) {
				sofar = 0;
				break;
			}

			// Process current command
			gcode_handler();

			// Send ready sign
			serial_ready();

			// Reset buffer
			sofar = 0;
		}
	}
}

/// <summary>
/// Prints ready sign to the serial port
/// </summary>
void serial_ready(void) {
	Serial.print(F(">"));
}
