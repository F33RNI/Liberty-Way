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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.log4j.Logger;

import java.util.ArrayList;

/**
 * This class stores an array of waypoints. It also provides the ability to add / remove waypoints using the API
 */
public class WaypointsContainer {
    private final Logger logger = Logger.getLogger(this.getClass().getSimpleName());

    public final static int WAYPOINTS_NUM = 16;

    public final static int WAYPOINT_SKIP = 0;
    public final static int WAYPOINT_PLATFORM = 1;
    public final static int WAYPOINT_FLY = 2;
    public final static int WAYPOINT_DESCENT = 3;
    public final static int WAYPOINT_PARCEL = 4;
    public final static int WAYPOINT_LAND = 5;

    public final static int CMD_BITS_SKIP = 0b000;
    public final static int CMD_BITS_DDC_NO_GPS_NO_DSC = 0b001;
    public final static int CMD_BITS_DDC_NO_DSC = 0b010;
    public final static int CMD_BITS_DDC = 0b011;
    public final static int CMD_BITS_FLY = 0b100;
    public final static int CMD_BITS_DESCEND = 0b101;
    public final static int CMD_BITS_PARCEL = 0b110;
    public final static int CMD_BITS_LAND = 0b110;

    private final ArrayList<Integer> waypointsAPI;
    private final ArrayList<GPS> waypointsGPS;
    private final ArrayList<Integer> waypointsCommand;

    WaypointsContainer() {
        waypointsAPI = new ArrayList<>();
        waypointsGPS = new ArrayList<>();
        waypointsCommand = new ArrayList<>();
    }

    /**
     * @return JsonArray of waypoints
     */
    public JsonArray getWaypointsAsJSON() {
        JsonArray waypoints = new JsonArray();
        for (int i = 0 ; i < waypointsAPI.size(); i++) {
            JsonObject waypoint = new JsonObject();

            // Add API tag to the JsonObject
            waypoint.addProperty("api", waypointsAPI.get(i));

            // Add GPS position if waypoint's API is not WAYPOINT_SKIP
            if (waypointsAPI.get(i) > WAYPOINT_SKIP) {
                waypoint.addProperty("lat", waypointsGPS.get(i).getLatDouble());
                waypoint.addProperty("lon", waypointsGPS.get(i).getLonDouble());
            }

            // Add 0 as GPS position if waypoint's API is WAYPOINT_SKIP
            else {
                waypoint.addProperty("lat", 0.0);
                waypoint.addProperty("lon", 0.0);
            }

            // Add JsonObject to the array
            waypoints.add(waypoint);
        }
        return waypoints;
    }

    /**
     * Adds new waypoint to the arrays
     * @param request JsonObject request including api as integer and lat and lot as strings
     * @return true if waypoint was added
     */
    public boolean addNewWaypoint(JsonObject request) {
        try {
            if (waypointsAPI.size() < 16) {
                int api = request.get("api").getAsInt();
                if (api >= WAYPOINT_SKIP) {
                    double latitude = Double.parseDouble(request.get("lat").getAsString());
                    double longitude = Double.parseDouble(request.get("lon").getAsString());
                    if (latitude >= -90.0 && latitude <= 90.0 && longitude >= -180.0 && longitude <= 180.0) {
                        waypointsAPI.add(api);
                        waypointsGPS.add(new GPS(latitude, longitude));
                        switch (api) {
                            case WAYPOINT_PLATFORM:
                                waypointsCommand.add(CMD_BITS_DDC);
                                break;
                            case WAYPOINT_FLY:
                                waypointsCommand.add(CMD_BITS_FLY);
                                break;
                            case WAYPOINT_DESCENT:
                                waypointsCommand.add(CMD_BITS_DESCEND);
                                break;
                            case WAYPOINT_PARCEL:
                                waypointsCommand.add(CMD_BITS_PARCEL);
                                break;
                            case WAYPOINT_LAND:
                                waypointsCommand.add(CMD_BITS_LAND);
                                break;
                            default:
                                waypointsCommand.add(CMD_BITS_SKIP);
                                break;
                        }
                        logger.info("Added new waypoint. Full list: " + getWaypointsAsJSON().toString());
                        return true;
                    }
                }
            }
        } catch (Exception ignored) { }
        return false;
    }

    public boolean deleteWaypoint(int index, boolean droneInFlight) {
        try {
            if (index >= WAYPOINT_SKIP && index < waypointsAPI.size()) {
                if (droneInFlight) {
                    waypointsAPI.set(index, WAYPOINT_SKIP);
                    waypointsCommand.set(index, CMD_BITS_SKIP);
                    logger.info("Skipped waypoint with ID: " + index +
                            " Full list: " + getWaypointsAsJSON().toString());
                } else {
                    waypointsAPI.remove(index);
                    waypointsGPS.remove(index);
                    waypointsCommand.remove(index);
                    logger.info("Removed waypoint with ID: " + index +
                            " Full list: " + getWaypointsAsJSON().toString());
                }
                return true;
            }
        } catch (Exception ignored) { }
        return false;
    }

    public void setPlatformPosition(GPS gps) {
        try {
            for (int i = 0 ; i < waypointsAPI.size(); i++) {
                if (waypointsAPI.get(i) == WAYPOINT_PLATFORM)
                    waypointsGPS.get(i).copyFromGPS(gps);
            }
        } catch (Exception ignored) { }
    }

    public int getWaypointsSize() {
        return waypointsAPI.size();
    }

    public ArrayList<Integer> getWaypointsAPI() {
        return waypointsAPI;
    }

    public ArrayList<GPS> getWaypointsGPS() {
        return waypointsGPS;
    }

    public ArrayList<Integer> getWaypointsCommand() {
        return waypointsCommand;
    }
}
