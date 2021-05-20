/*
 * Copyright (C) 2021 Kabalin Andrey, Liberty-Way Landing System Project
 *
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

package com.liberty_amls;

import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;

public class GPSEstimationHandler {
    private final PlatformContainer platformContainer;

    private int estimatedGPSLat = 0, estimatedGPSLon = 0;

    /**
     * This class operates with GPS coordinates and predicts the next that the drone will receive
     * @param platformContainer Container of telemetry's parameters
     */
    public GPSEstimationHandler(PlatformContainer platformContainer){ this.platformContainer = platformContainer; }

    /**
     * Handles all of the calculations in order to predict the future GPS coordinate
     */
    public void calculate() {
        ArrayList<Double> ksX = new ArrayList<>();
        ArrayList<Double> ksY = new ArrayList<>();
        int latError = 0, lonError = 0;

        ArrayList<Integer> trueGPSLat = platformContainer.trueGPSLat;
        ArrayList<Integer> trueGPSLon = platformContainer.trueGPSLon;

        int currentLat = platformContainer.gpsLatInt;
        int currentLon = platformContainer.gpsLonInt;

        if (this.estimatedGPSLat != 0 && this.estimatedGPSLon != 0){
            latError = currentLat - this.estimatedGPSLat;
            lonError = currentLon - this.estimatedGPSLon;
        }

        for (int i = 0; i < trueGPSLat.size() - 1; i++){
            ksX.add(trueGPSLat.get(i + 1) / (double)trueGPSLat.get(i));
            ksY.add(trueGPSLon.get(i + 1) / (double)trueGPSLon.get(i));
        }

        DoubleSummaryStatistics stats = ksX.stream().mapToDouble((x) -> x).summaryStatistics();
        double avgXK = stats.getAverage();

        stats = ksY.stream().mapToDouble((x) -> x).summaryStatistics();
        double avgYK = stats.getAverage();

        double x_omega = ksX.get(ksX.size() - 1) - avgXK;
        double y_omega = ksY.get(ksY.size() - 1) - avgYK;

        double alphaX = platformContainer.alphaX;
        double alphaY = platformContainer.alphaY;

        int eLat = (int) (-alphaX*x_omega + currentLat*avgXK + latError);
        int eLon = (int) (-alphaY*y_omega + currentLon*avgYK + lonError);

        this.estimatedGPSLat = eLat;
        this.estimatedGPSLon = eLon;
    }

    /**
     * This method returns the last
     * GPS coordinate that has been
     * calculated to be next (latitude)
     * @return the last EstimatedGPS' latitude
     */
    public int getEstimatedGPSLat(){
        return this.estimatedGPSLat;
    }

    /**
     * This method returns the last
     * GPS coordinate that has been
     * calculated to be next (longitude)
     * @return the last EstimatedGPS' longitude
     */
    public int getEstimatedGPSLon(){
        return this.estimatedGPSLon;
    }
}