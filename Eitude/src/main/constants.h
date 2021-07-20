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

#ifndef CONSTANTS_H
#define CONSTANTS_H

 // The loop frequency is 40 Hz. Changing it may cause system break or even injury
const uint32_t LOOP_PERIOD PROGMEM = 25000;
const uint32_t MAX_ALLOWED_LOOP_PERIOD PROGMEM = 26000;

// Hardware constants
const uint8_t IMU_ADDRESS PROGMEM = 0x68;

// Serial port
const uint16_t BUFFER_SIZE PROGMEM = 256;

#endif