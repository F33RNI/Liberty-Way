/**
 * Copyright 2021 Kabalin Andrey, The Liberty-Way Landing System Open Source Project
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

package com.liberty_amls;

import java.util.ArrayList;
import java.util.IntSummaryStatistics;

public class GPSEstimationHandler {
    private final GPSEstimationContainer gpsEstimationContainer;
    private final TelemetryContainer telemetryContainer;

    /**
     * This class operates with GPS coordinates and predicts the next that the drone will receive
     * @param gpsEstimationContainer GPS coordinates container
     */
    public GPSEstimationHandler(GPSEstimationContainer gpsEstimationContainer,
                                TelemetryContainer telemetryContainer){
        this.gpsEstimationContainer = gpsEstimationContainer;
        this.telemetryContainer = telemetryContainer;
    }

    /**
     * Handles all of the calculation in order to predict the future GPS coordinate
     */
    private void Calculate() {
        ArrayList<Integer> x_ks = new ArrayList<Integer>();
        ArrayList<Integer> y_ks = new ArrayList<Integer>();
        int lat_error = 0, lon_error = 0;

        ArrayList<GPSEstimationContainer.EstimatedGPS> estimatedGPSList =
                gpsEstimationContainer.arrayOfEstimatedGPS;

        ArrayList<GPSEstimationContainer.TrueGPS> trueGPSList =
                gpsEstimationContainer.arrayOfTrueGPS;

        int current_lat = trueGPSList.get(trueGPSList.size() - 1).latitude;
        int current_lon = trueGPSList.get(trueGPSList.size() - 1).longitude;

        if (estimatedGPSList.size() > 0){
            lat_error = current_lat - estimatedGPSList.get(estimatedGPSList.size() - 1).latitude;
            lon_error = current_lon - estimatedGPSList.get(estimatedGPSList.size() - 1).longitude;
        }

        for (int i = 0; i < trueGPSList.size() - 1; i++){
            x_ks.add(trueGPSList.get(i + 1).latitude / trueGPSList.get(i).latitude);
            y_ks.add(trueGPSList.get(i + 1).longitude / trueGPSList.get(i).longitude);
        }

        IntSummaryStatistics stats = x_ks.stream().mapToInt((x) -> x).summaryStatistics();
        int x_avg_k = (int)stats.getAverage();

        stats = y_ks.stream().mapToInt((x) -> x).summaryStatistics();
        int y_avg_k = (int)stats.getAverage();

        int x_omega = x_ks.get(x_ks.size() - 1) - x_avg_k;
        int y_omega = y_ks.get(y_ks.size() - 1) - y_avg_k;

        var velocity_x = telemetryContainer.velocity.velocityX;
        var velocity_y = telemetryContainer.velocity.velocityY;
        var freq = telemetryContainer.loopTime;

        double k = gpsEstimationContainer.estimationCoefficient;
        int e_lat = (int) ((-freq*(velocity_x / 0.036)*x_omega + current_lat*x_avg_k + lat_error) * k);
        int e_lon = (int) ((-freq*(velocity_y / 0.036)*y_omega + current_lon*y_avg_k + lon_error) * k);

        gpsEstimationContainer.arrayOfEstimatedGPS.add(
                new GPSEstimationContainer.EstimatedGPS(e_lat, e_lon));
    }
}