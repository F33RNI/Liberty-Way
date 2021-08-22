/*
 * Copyright (C) 2021 Fern Hertz (Pavel Neshumov), Liberty-Way Landing System Project
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

package com.liberty_amls;

public class GPSPredictor {
    private final GPS gpsLast;
    private final GPS gpsCurrent;
    private final GPS gpsPredicted;
    private double compassHeading;

    /**
     * This class calculates a line between the current and previous GPS coordinates
     * and returns the "predicted" GPS coordinates that belong to this line at a distance of "increment"
     */
    GPSPredictor() {
        this.gpsLast = new GPS();
        this.gpsCurrent = new GPS();
        this.gpsPredicted = new GPS();
        this.compassHeading = 0.0;
    }

    /**
     * Calculates compass heading (in radians) using 2 GPS points
     */
    public void calculateHeadingFromGPS() {
        if (gpsLast.isNotEmpty() && gpsCurrent.isNotEmpty()) {
            // Calculate the deviation between the previous and current points
            int dLat = gpsLast.getLatInt() - gpsCurrent.getLatInt();
            int dLon = gpsLast.getLonInt() - gpsCurrent.getLonInt();

            // Calculate compass heading
            if (dLat != 0 || dLon != 0)
                compassHeading = Math.atan2(dLat, dLon) + Math.PI / 2;
        }
    }

    /**
     * Roughly predicts new GPS coordinates
     * @return predicted GPS coordinates
     */
    public GPS getGPSPredicted() {
        double increment;
        if (gpsLast.isNotEmpty() && gpsCurrent.isNotEmpty()) {
            // Calculate the deviation between the previous and current points
            int dLat = gpsLast.getLatInt() - gpsCurrent.getLatInt();
            int dLon = gpsLast.getLonInt() - gpsCurrent.getLonInt();

            // Calculate increment for next point
            increment = Math.sqrt(dLat * dLat + dLon + dLon);
        } else
            increment = 0;

        gpsPredicted.setFromInt(gpsCurrent.getLatInt() + (int)(increment * Math.cos(compassHeading)),
                gpsCurrent.getLonInt() + (int)(increment * -Math.sin(compassHeading)));
        return gpsPredicted;
    }

    /**
     * @return compass heading in radians
     */
    public double getCompassHeading() {
        return compassHeading;
    }

    /**
     * @param gpsLast previous GPS coordinates
     */
    public void setGPSLast(GPS gpsLast) {
        this.gpsLast.setFromInt(gpsLast.getLatInt(), gpsLast.getLonInt());
    }

    /**
     * @param GPSCurrent current GPS coordinates
     */
    public void setGPSCurrent(GPS GPSCurrent) {
        this.gpsCurrent.setFromInt(GPSCurrent.getLatInt(), GPSCurrent.getLonInt());
    }

    /**
     * @param compassHeadingRadians compass angle in radians
     */
    public void setCompassHeading(double compassHeadingRadians) {
        this.compassHeading = compassHeadingRadians;
    }
}
