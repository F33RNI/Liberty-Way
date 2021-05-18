/**
 * Copyright 2021 Kabalin, Andrey The Liberty-Way Landing System Open Source Project
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

public class GPSEstimationContainer {
    public ArrayList<EstimatedGPS> arrayOfEstimatedGPS;
    public ArrayList<TrueGPS> arrayOfTrueGPS;
    public double estimationCoefficient;

    /**
     * This class contains all data about platform's GPS position
     * and the coefficient of estimation efficiency
     */
    public GPSEstimationContainer(){
        // initialize ArrayLists
        arrayOfTrueGPS = new ArrayList<>();
        arrayOfEstimatedGPS = new ArrayList<>();
        estimationCoefficient = 1;
    }


    static class EstimatedGPS{
        public final int latitude, longitude;

        /**
         * This sub-class contains latitude and longitude of 1 predicted GPS coordinate
         * @param lat Predicted latitude
         * @param lon Predicted longitude
         */
        public EstimatedGPS(int lat, int lon){
            this.latitude = lat;
            this.longitude = lon;
        }
    }


    static class TrueGPS{
        public final int latitude, longitude;

        /**
         * This sub-class contains latitude and longitude of 1 received (true) GPS coordinate
         * @param lat True latitude
         * @param lon True longitude
         */
        public TrueGPS(int lat, int lon) {
            this.latitude = lat;
            this.longitude = lon;
        }
    }
}
