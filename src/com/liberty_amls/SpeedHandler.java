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

public class SpeedHandler {
    private final SettingsContainer settingsContainer;
    private final GPS gpsCurrent;
    private final GPS gpsLast;
    private double speedLast;
    private long millisCurrent;
    private long millisLast;

    /**
     * This class calculates speed (in km/h) from GPS coordinates and time difference
     * @param settingsContainer SettingContainer class that stores the radius of the planet
     */
    SpeedHandler(SettingsContainer settingsContainer) {
        this.settingsContainer = settingsContainer;
        this.gpsCurrent = new GPS();
        this.gpsLast = new GPS();
        this.speedLast = 0.0;
        this.millisCurrent = -1;
        this.millisLast = -1;
    }

    /**
     * Remembers previous GPS coordinates
     * @param gpsLast Previous GPS coordinates
     */
    public void feedLast(GPS gpsLast) {
        this.gpsLast.setFromInt(gpsLast.getLatInt(), gpsLast.getLonInt());
    }

    /**
     * Remembers new GPS coordinates and timestamp
     * @param gpsCurrent New GPS coordinates
     * @param millisCurrent Time in milliseconds of new GPS measurement
     */
    public void feedCurrent(GPS gpsCurrent, long millisCurrent) {
        // Store previous time as millisLast
        this.millisLast = this.millisCurrent;

        // Set new values
        this.gpsCurrent.setFromInt(gpsCurrent.getLatInt(), gpsCurrent.getLonInt());
        this.millisCurrent = millisCurrent;

        // First run
        if (this.millisLast == -1)
            this.millisLast = this.millisCurrent;
    }

    /**
     * Calculates speed in km/h
     * @return Speed in km/h
     */
    public double getSpeed() {
        // Check time difference
        if (millisCurrent == millisLast)
            return 0;

        // Find distance between two points on geoid
        double distance = distanceOnGeoid(gpsLast, gpsCurrent, settingsContainer.planetRadius);

        // Convert time diff to seconds
        double timeInSeconds = (millisCurrent - millisLast) / 1000.0;

        // Calculate speed in M/s
        double speedMps = distance / timeInSeconds;

        // Convert to KM/H with filter
        double speed = speedLast * settingsContainer.speedFilter +
                ((speedMps * 3600.0) / 1000.0) * (1 - settingsContainer.speedFilter);

        speedLast = speed;
        return speed;
    }

    /**
     * Calculates distance on geoid using GPS coordinates
     * @param position1 First GPS position
     * @param position2 Second GPS position
     * @param planetRadius Radius of a current planet that the project operates on
     * @return Distance between two points in meters
     */
    public static double distanceOnGeoid(GPS position1, GPS position2, double planetRadius) {
        // Check if both coordinates presented
        if (position1.isNotEmpty() && position2.isNotEmpty()) {
            // Find distance in radians
            double latDistance = Math.toRadians(position2.getLatDouble() - position1.getLatDouble());
            double lonDistance = Math.toRadians(position2.getLonDouble() - position1.getLonDouble());

            // Convert from geoid coordinates to meters
            double a = Math.sin(latDistance / 2.0) * Math.sin(latDistance / 2.0)
                    + Math.cos(Math.toRadians(position1.getLatDouble()))
                    * Math.cos(Math.toRadians(position1.getLonDouble()))
                    * Math.sin(lonDistance / 2.0) * Math.sin(lonDistance / 2.0);
            return planetRadius * 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a)) * 1000;
        }
        return 0;
    }
}
