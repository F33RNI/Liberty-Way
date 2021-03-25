/**
 * Copyright 2021 The Liberty-Way Landing System Open Source Project
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
 */

package liberty_amls;

import java.util.ArrayList;
import java.util.LongSummaryStatistics;

public class GPSEstimationHandler implements Runnable {

    /**FREQUENCY is a crutch, should be replaced with something more fit
     * but i'm not sure
     * until we find a solution this will be a default GPS update rate = 200 ms*/
    private final double FREQUENCY = 0.2;
    private final GPSEstimationContainer gpsEstimationContainer;
    volatile public boolean receivedGPS = false;

    /**
     * This class operates with GPS coordinates and predicts the next that the drone will receive
     * @param gpsEstimationContainer GPS coordinates container
     */
    public GPSEstimationHandler(GPSEstimationContainer gpsEstimationContainer){
        this.gpsEstimationContainer = gpsEstimationContainer;
    }


    /**
     * Starts the main loop
     */
    @Override
    public void run() {
        if (receivedGPS) {
            if (gpsEstimationContainer.arrayOfTrueGPS.size() > 4)
                Calculate();
        }
    }

    /**
     * Handles all of the calculation in order to predict the future GPS coordinate
     */
    private void Calculate() {
        ArrayList<Long> x_ks = new ArrayList<>();
        ArrayList<Long> y_ks = new ArrayList<>();
        long lat_error = 0, lon_error = 0;

        ArrayList<GPSEstimationContainer.EstimatedGPS> estimatedGPSList =
                gpsEstimationContainer.arrayOfEstimatedGPS;

        ArrayList<GPSEstimationContainer.TrueGPS> trueGPSList =
                gpsEstimationContainer.arrayOfTrueGPS;

        long current_lat = trueGPSList.get(trueGPSList.size() - 1).latitude;
        long current_lon = trueGPSList.get(trueGPSList.size() - 1).longitude;

        long velocity_x = (long) (1 / FREQUENCY * Math.abs(current_lat
                - trueGPSList.get(trueGPSList.size() - 2).latitude
        ));

        long velocity_y = (long) (1 / FREQUENCY * Math.abs(current_lon
                - trueGPSList.get(trueGPSList.size() - 2).longitude
        ));

        if (estimatedGPSList.size() > 0){
            lat_error = current_lat - estimatedGPSList.get(estimatedGPSList.size() - 1).latitude;
            lon_error = current_lon - estimatedGPSList.get(estimatedGPSList.size() - 1).longitude;
        }

        for (int i = 0; i < trueGPSList.size() - 1; i++){
            x_ks.add(trueGPSList.get(i + 1).latitude / trueGPSList.get(i).latitude);
            y_ks.add(trueGPSList.get(i + 1).longitude / trueGPSList.get(i).longitude);
        }

        LongSummaryStatistics stats = x_ks.stream().mapToLong((x) -> x).summaryStatistics();
        long x_avg_k = (long)stats.getAverage();

        stats = y_ks.stream().mapToLong((x) -> x).summaryStatistics();
        long y_avg_k = (long)stats.getAverage();

        long x_omega = x_ks.get(x_ks.size() - 1) - x_avg_k;
        long y_omega = y_ks.get(y_ks.size() - 1) - y_avg_k;

        long e_lat = (long) (-FREQUENCY*velocity_x*x_omega + current_lat*x_avg_k + lat_error);
        long e_lon = (long) (-FREQUENCY*velocity_y*y_omega + current_lon*y_avg_k + lon_error);

        gpsEstimationContainer.arrayOfEstimatedGPS.add(
                new GPSEstimationContainer.EstimatedGPS(e_lat, e_lon));
    }
}