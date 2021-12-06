package com.liberty_amls;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.log4j.Logger;

import java.util.ArrayList;

public class WaypointsContainer {
    private final Logger logger = Logger.getLogger(this.getClass().getSimpleName());

    public final static int WAYPOINT_SKIP = 0;
    public final static int WAYPOINT_PLATFORM = 1;
    public final static int WAYPOINT_FLY = 2;
    public final static int WAYPOINT_DESCENT = 3;
    public final static int WAYPOINT_PARCEL = 4;
    public final static int WAYPOINT_LAND = 5;

    private final static int CMD_BITS_SKIP = 0b000;
    private final static int CMD_BITS_DDC_NO_GPS_NO_DSC = 0b001;
    private final static int CMD_BITS_DDC_NO_DSC = 0b010;
    private final static int CMD_BITS_DDC = 0b011;
    private final static int CMD_BITS_FLY = 0b100;
    private final static int CMD_BITS_DESCEND = 0b101;
    private final static int CMD_BITS_PARCEL = 0b110;
    private final static int CMD_BITS_LAND = 0b110;

    public ArrayList<Integer> waypointsAPI;
    public ArrayList<GPS> waypointsGPS;
    public ArrayList<Integer> waypointsCommand;


    WaypointsContainer() {
        waypointsAPI = new ArrayList<>();
        waypointsGPS = new ArrayList<>();
        waypointsCommand = new ArrayList<>();
    }

    public JsonArray getWaypointsAsJSON() {
        JsonArray waypoints = new JsonArray();
        for (int i = 0 ; i < waypointsAPI.size(); i++) {
            JsonObject waypoint = new JsonObject();
            waypoint.addProperty("api", waypointsAPI.get(i));
            if (waypointsAPI.get(i) > WAYPOINT_SKIP) {
                waypoint.addProperty("lat", waypointsGPS.get(i).getLatDouble());
                waypoint.addProperty("lon", waypointsGPS.get(i).getLonDouble());
            }
            waypoints.add(waypoint);
        }
        return waypoints;
    }

    public boolean addNewWaypoint(JsonObject request) {
        try {
            if (waypointsAPI.size() < 16) {
                int api = request.get("api").getAsInt();
                if (api >= WAYPOINT_SKIP) {
                    double latitude = Double.parseDouble(request.get("lat").getAsString());
                    double longitude = Double.parseDouble(request.get("lon").getAsString());
                    if (latitude >= -90.0 && latitude <= 90.0 && longitude >= -180.0 && longitude <= 180.0) {
                        waypointsGPS.add(new GPS(latitude, longitude));
                    } else
                        return false;

                    waypointsAPI.add(api);

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
                    return true;
                } else {
                    waypointsAPI.remove(index);
                    waypointsGPS.remove(index);
                    waypointsCommand.remove(index);
                    logger.info("Removed waypoint with ID: " + index +
                            " Full list: " + getWaypointsAsJSON().toString());
                    return true;
                }
            }
        } catch (Exception ignored) { }
        return false;
    }
}
