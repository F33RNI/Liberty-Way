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

public class GPS {
    private int latInt, lonInt;
    private double latDouble, lonDouble;
    private boolean initialized;

    /**
     * This class stores GPS coordinates and allows conversions between Integer and Double types
     */
    GPS() {
        this.latInt = 0;
        this.lonInt = 0;
        this.latDouble = 0.0;
        this.lonDouble = 0.0;
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
}
