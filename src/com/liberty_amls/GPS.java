/*
 * Copyright (C) 2021 Fern H. (aka Pavel Neshumov), Liberty-Way Landing System Project
 * This software is part of Liberty Drones Project aka AMLS (Autonomous Multirotor Landing System)
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

public class GPS {
    private int latInt, lonInt;
    private double latDouble, lonDouble;
    private int satellitesNum;
    private double groundSpeed;
    private double groundHeading;
    private boolean initialized;

    /**
     * This class stores GPS coordinates, number of satellites, ground speed and ground heading
     * and allows conversions between Integer and Double types
     */
    GPS() {
        this.latInt = 0;
        this.lonInt = 0;
        this.latDouble = 0.0;
        this.lonDouble = 0.0;
        this.satellitesNum = 0;
        this.groundSpeed = 0.0;
        this.groundHeading = 0.0;
        this.initialized = false;
    }

    /**
     * Stores new GPS coordinates from Integer type
     * @param lat value between -90000000 and 90000000
     * @param lon value between -180000000 and 180000000
     */
    public void setFromInt(int lat, int lon) {
        latInt = lat;
        lonInt = lon;
        latDouble = (double) latInt / 1000000.0;
        lonDouble = (double) lonInt / 1000000.0;
        initialized = true;
    }

    /**
     * Stores new GPS coordinates from Double type
     * @param lat value between -90.000000 and 90.000000
     * @param lon value between -180.000000 and 180.000000
     */
    public void setFromDouble(double lat, double lon) {
        latDouble = lat;
        lonDouble = lon;
        latInt = (int) (latDouble * 1000000.0);
        lonInt = (int) (lonDouble * 1000000.0);
        initialized = true;
    }

    /**
     * @return latitude as Integer
     */
    public int getLatInt() {
        return latInt;
    }

    /**
     * @return longitude as Integer
     */
    public int getLonInt() {
        return lonInt;
    }

    /**
     * @return latitude as Double
     */
    public double getLatDouble() {
        return latDouble;
    }

    /**
     * @return longitude as Double
     */
    public double getLonDouble() {
        return lonDouble;
    }

    /**
     * @return true if the GPS class is initialized
     */
    public boolean isNotEmpty() {
        return initialized;
    }

    /**
     * @return number of satellites
     */
    public int getSatellitesNum() {
        return satellitesNum;
    }

    /**
     * Sets new number of active satellites
     * @param satellitesNum integer number of satellites
     */
    public void setSatellitesNum(int satellitesNum) {
        this.satellitesNum = satellitesNum;
    }

    /**
     * @return ground speed in km/h
     */
    public double getGroundSpeed() {
        return groundSpeed;
    }

    /**
     * Sets new ground speed
     * @param groundSpeed speed in km/h
     */
    public void setGroundSpeed(double groundSpeed) {
        this.groundSpeed = groundSpeed;
    }

    /**
     * @return ground heading (yaw angle) in degrees
     */
    public double getGroundHeading() {
        return groundHeading;
    }

    /**
     * Sets new ground heading (yaw angle)
     * @param groundHeading angle in degrees
     */
    public void setGroundHeading(double groundHeading) {
        this.groundHeading = groundHeading;
    }

    /**
     * Copies all class parameters from another GPS class
     * @param gps GPS class
     */
    public void copyFromGPS(GPS gps) {
        this.latInt = gps.getLatInt();
        this.lonInt = gps.getLonInt();
        this.latDouble = gps.getLatDouble();
        this.lonDouble = gps.getLonDouble();
        this.satellitesNum = gps.getSatellitesNum();
        this.groundSpeed = gps.getGroundSpeed();
        this.groundHeading = gps.getGroundHeading();
        this.initialized = gps.isNotEmpty();
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
