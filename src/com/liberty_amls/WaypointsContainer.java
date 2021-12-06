package com.liberty_amls;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;

public class WaypointsContainer {
    public final static int WAYPOINT_SKIP = 0;
    public final static int WAYPOINT_PLATFORM = 1;
    public final static int WAYPOINT_FLY = 2;
    public final static int WAYPOINT_DESCENT = 3;
    public final static int WAYPOINT_PARCEL = 4;
    public final static int WAYPOINT_LAND = 5;

    public ArrayList<Integer> waypointsAPI;
    public ArrayList<GPS> waypointsGPS;
    public ArrayList<Integer> waypointsCommand;


    WaypointsContainer() {
        waypointsAPI = new ArrayList<>();
        waypointsGPS = new ArrayList<>();
        waypointsCommand = new ArrayList<>();


        JsonObject test = new JsonObject();
        test.addProperty("api", 2);
        test.addProperty("lat", 12.254865);
        test.addProperty("lon", -8.967539);
        addNewWaypoint(test);

        test = new JsonObject();
        test.addProperty("api", 2);
        test.addProperty("lat", 13.254865);
        test.addProperty("lon", -8.967539);
        addNewWaypoint(test);

        Main.logger.warn(getWaypointsAsJSON().toString());

    }

    public JsonArray getWaypointsAsJSON() {
        JsonArray waypoints = new JsonArray();
        for (int i = 0 ; i < waypointsAPI.size(); i++) {
            JsonObject waypoint = new JsonObject();
            waypoint.addProperty("api", waypointsAPI.get(i));
            if (waypointsAPI.get(i) > 0) {
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
                if (api > WAYPOINT_PLATFORM) {
                    double latitude = request.get("lat").getAsDouble();
                    double longitude = request.get("lon").getAsDouble();
                    if (latitude >= -90.0 && latitude <= 90.0 && longitude >= -180.0 && longitude <= 180.0) {
                        waypointsGPS.add(new GPS(latitude, longitude));
                    }
                }
                if (api >= WAYPOINT_SKIP) {
                    waypointsAPI.add(api);
                    switch (api) {
                        case WAYPOINT_PLATFORM:
                            waypointsCommand.add(0b011);
                            break;
                        case WAYPOINT_FLY:
                            waypointsCommand.add(0b100);
                            break;
                        case WAYPOINT_DESCENT:
                            waypointsCommand.add(0b101);
                            break;
                        case WAYPOINT_PARCEL:
                            waypointsCommand.add(0b110);
                            break;
                        case WAYPOINT_LAND:
                            waypointsCommand.add(0b111);
                            break;
                        default:
                            waypointsCommand.add(0b000);
                            break;
                    }
                    return true;
                }
            }
        } catch (Exception ignored) { }
        return false;
    }

    public boolean deleteWaypoint(int index, boolean droneInFlight) {
        return false;
    }
}
