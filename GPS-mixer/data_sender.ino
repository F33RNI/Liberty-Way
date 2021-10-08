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

/// <summary>
/// Sends data to OUTPUT_SERIAL by BURST_BYTES blocks
/// </summary>
void data_sender(void) {
    if (new_data_flag) {
        if (TRANSMIT_DELAY > 0) {
            if (micros() - output_timer_micros >= TRANSMIT_DELAY) {
                output_timer_micros = micros();
                if (output_buffer_position == 20) {
                    new_data_flag = 0;
                    return;
                }
                else {
                    OUTPUT_SERIAL.write(output_buffer[output_buffer_position]);
                    output_buffer_position++;
                }
            }
        }
        else {
            OUTPUT_SERIAL.write(output_buffer, 20);
            new_data_flag = 0;
        }

        /*OUTPUT_SERIAL.print(working_receivers_num_last);
        OUTPUT_SERIAL.print(',');
        OUTPUT_SERIAL.print(l_lat_avg);
        OUTPUT_SERIAL.print(',');
        OUTPUT_SERIAL.println(l_lon_avg);
        new_data_flag = 0;*/
    }
}
