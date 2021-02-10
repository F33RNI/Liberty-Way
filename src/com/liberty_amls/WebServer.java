/*
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

package com.liberty_amls;

import com.google.gson.*;
import flak.App;
import flak.Flak;
import flak.Query;
import flak.Response;
import flak.annotations.Route;
import flak.plugin.resource.FlakResourceImpl;
import org.apache.log4j.Logger;
import org.opencv.core.Core;

import javax.swing.*;
import java.net.InetAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class WebServer {
    private final Logger logger = Logger.getLogger(this.getClass().getSimpleName());
    private App app;
    private final String hostName;
    private final int serverPort;
    private final int videoPort;
    private boolean controllerRunning = false, aborted = false;
    private OpenCVHandler openCVHandler;
    private OSDHandler osdHandler;
    private UDPHandler udpHandler;
    private SerialHandler serialHandler;
    private PositionHandler positionHandler;

    /**
     * This class creates a server (based on the Flak library) to control the landing process
     * @param hostName server IP
     * @param serverPort server (controller) Port
     * @param videoPort http video stream port
     */
    public WebServer(String hostName, int serverPort, int videoPort) {
        this.videoPort = videoPort;
        this.serverPort = serverPort;
        this.hostName = hostName;
    }

    /**
     * Video Stream page
     */
    @Route("/video_feed")
    public void video_feed(Response resp) {
        // Redirect hostName:serverPort/video_feed to hostName:videoPort
        resp.redirect(app.getRootUrl().replace(
                app.getRootUrl().split(":")[app.getRootUrl().split(":").length - 1],
                String.valueOf(videoPort)));
    }

    /**
     * Main page
     */
    @Route("/")
    public String index() {
        String content;
        if (aborted)
            // Load connection_closed page if aborted flag provided
            content = FileWorkers.loadString(
                    Main.settings.get("web_templates_folder").getAsString() + "/connection_closed.html");
        else if (!controllerRunning)
            // Load index (main) page if controller is not running
            content = FileWorkers.loadString(
                    Main.settings.get("web_templates_folder").getAsString() + "/index.html");
        else
            // Load controller page if it was started
            content = FileWorkers.loadString(
                    Main.settings.get("web_templates_folder").getAsString() + "/controller.html");

        if (aborted)
            // Just return page if aborted flag provided
            return content;

        if (!controllerRunning) {
            // index.html
            // Scan available serial ports
            StringBuilder serialPortsSelector = new StringBuilder();
            List<String> serialPorts = SerialHandler.getPortNames();
            logger.info("Discovered serial ports: " + serialPorts);

            // Creates a drop-down list (select html tag)
            for (String serialPort : serialPorts) {
                serialPortsSelector.append("<option value=\"")
                        .append(serialPort)
                        .append("\">")
                        .append(serialPort)
                        .append("</option>");
            }

            // Put ports into html page
            content = content.replace("{{ cnc_ports }}", serialPortsSelector.toString());
            content = content.replace("{{ rf_ports }}", serialPortsSelector.toString());
        } else {
            // controller.html
            // Load PIDs from file
            JsonObject pids = FileWorkers.loadJsonObject(Main.settings.get("pid_file").getAsString());

            // Set coefficients for MiniPID in PositionHandler class
            positionHandler.setPIDFromJson(pids);

            // Put coefficients into html page
            content = content.replace("{{ pid_roll.p }}",
                    pids.get("pid_x").getAsJsonObject().get("P").getAsString());
            content = content.replace("{{ pid_roll.i }}",
                    pids.get("pid_x").getAsJsonObject().get("I").getAsString());
            content = content.replace("{{ pid_roll.d }}",
                    pids.get("pid_x").getAsJsonObject().get("D").getAsString());
            content = content.replace("{{ pid_alt.p }}",
                    pids.get("pid_z").getAsJsonObject().get("P").getAsString());
            content = content.replace("{{ pid_alt.i }}",
                    pids.get("pid_z").getAsJsonObject().get("I").getAsString());
            content = content.replace("{{ pid_alt.d }}",
                    pids.get("pid_z").getAsJsonObject().get("D").getAsString());
            // Check switches on html if stabilization or landing is enabled
            content = content.replace("{{ holding_drone }}", positionHandler.stabEnabled ? "checked" : "");
            content = content.replace("{{ landing_drone }}", positionHandler.landingEnabled ? "checked" : "");

            // Check 'Enable video' checkbox on html if video stream is enabled
            content = content.replace("{{ debug_info.v }}",
                    positionHandler.osdHandler.streamEnabled ? "checked" : "");
        }
        return content;
    }

    /**
     * Start the controller (runs only by index.html)
     */
    @Route("/start")
    @SuppressWarnings("StatementWithEmptyBody")
    public void startController(Response resp) {
        try {
            Query query = resp.getRequest().getQuery();
            // Log all data submitted from the form
            logger.info("Starting the controller.");
            logger.info("CNC Port: " + query.get("cnc_port"));
            logger.info("CNC Port baudrate: " + query.get("cnc_baudrate"));
            logger.info("RF Port: " + query.get("rf_port"));
            logger.info("RF Port baudrate: " + query.get("rf_baudrate"));
            logger.info("UDP Ip/Port: " + query.get("udp_ip_port"));
            logger.info("Camera ID: " + query.get("camera_id"));
            logger.info("Exposure: " + query.get("camera_exp"));

            // Load native library (from java-library-path)
            logger.info("Loading OpenCV Native Library.");
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

            // Create SerialHandler class for serial communication
            serialHandler = new SerialHandler(query.get("cnc_port"), query.get("cnc_baudrate"),
                    query.get("rf_port"), query.get("rf_baudrate"));
            serialHandler.openPorts();

            // Create UDPHandler class for UDP communication
            udpHandler = new UDPHandler(query.get("udp_ip_port"));
            udpHandler.openUDP();

            // Create OSDHandler and VideoStream classes
            osdHandler = new OSDHandler(new VideoStream(null, InetAddress.getByName(hostName), videoPort));
            // Create and start a new stream with the lowest priority for video stream
            Thread osdThread = new Thread(osdHandler);
            osdThread.setPriority(Thread.MIN_PRIORITY);
            osdThread.start();

            // Create PositionHandler class for to handle the current position
            positionHandler = new PositionHandler(serialHandler, udpHandler, osdHandler);
            positionHandler.landingAltitude = Main.settings.get("landing_alt").getAsDouble();
            positionHandler.loadSettings(Main.settings);

            // Create OpenCVHandler class for find marker and estimate its position
            openCVHandler = new OpenCVHandler(Integer.parseInt(query.get("camera_id")),
                    Integer.parseInt(query.get("camera_exp")),
                    Main.settings.get("marker_size").getAsFloat(),
                    positionHandler);
            openCVHandler.start();

            // Create and start a new stream with the highest priority for opencv handler
            Thread openCVThread = new Thread(openCVHandler);
            openCVThread.setPriority(Thread.MAX_PRIORITY);
            openCVThread.start();

            // Wait for the first frame from OpenCVHandler
            while (openCVHandler.frame == null || openCVHandler.frame.empty());

            // Set controllerRunning flag
            controllerRunning = true;
            logger.info("Controller startup is complete.");
            logger.info("Redirecting...");

            // Redirect to the main page (controller.html)
            resp.redirect("/");
        } catch (Exception e) {
            logger.error("Error starting the controller!", e);
            System.exit(1);
        }
    }

    /**
     * Setup PID and debug info (runs only by controller.html)
     */
    @Route("/setup")
    public void setup(Response resp) {
        Query query = resp.getRequest().getQuery();
        JsonObject pidsNew = new JsonObject();
        // Load current coefficients from file
        JsonObject pidsOld = FileWorkers.loadJsonObject(Main.settings.get("pid_file").getAsString());

        // Combine PIX X with the new values
        JsonObject pid = new JsonObject();
        pid.add("P", new JsonPrimitive(Double.parseDouble(query.get("pid_roll_p"))));
        pid.add("I", new JsonPrimitive(Double.parseDouble(query.get("pid_roll_i"))));
        pid.add("D", new JsonPrimitive(Double.parseDouble(query.get("pid_roll_d"))));
        pid.add("F", pidsOld.get("pid_x").getAsJsonObject().get("F"));
        pid.add("limit", pidsOld.get("pid_x").getAsJsonObject().get("limit"));
        pid.add("reversed", pidsOld.get("pid_x").getAsJsonObject().get("reversed"));
        pidsNew.add("pid_x", pid);

        // Combine PIX Y with the new values
        pid = new JsonObject();
        pid.add("P", new JsonPrimitive(Double.parseDouble(query.get("pid_roll_p"))));
        pid.add("I", new JsonPrimitive(Double.parseDouble(query.get("pid_roll_i"))));
        pid.add("D", new JsonPrimitive(Double.parseDouble(query.get("pid_roll_d"))));
        pid.add("F", pidsOld.get("pid_y").getAsJsonObject().get("F"));
        pid.add("limit", pidsOld.get("pid_y").getAsJsonObject().get("limit"));
        pid.add("reversed", pidsOld.get("pid_y").getAsJsonObject().get("reversed"));
        pidsNew.add("pid_y", pid);

        // Combine PIX Z with the new values
        pid = new JsonObject();
        pid.add("P", new JsonPrimitive(Double.parseDouble(query.get("pid_alt_p"))));
        pid.add("I", new JsonPrimitive(Double.parseDouble(query.get("pid_alt_i"))));
        pid.add("D", new JsonPrimitive(Double.parseDouble(query.get("pid_alt_d"))));
        pid.add("F", pidsOld.get("pid_z").getAsJsonObject().get("F"));
        pid.add("limit", pidsOld.get("pid_z").getAsJsonObject().get("limit"));
        pid.add("reversed", pidsOld.get("pid_z").getAsJsonObject().get("reversed"));
        pidsNew.add("pid_z", pid);

        // Combine PIX Yaw with the new values
        pidsNew.add("pid_yaw", pidsOld.get("pid_yaw").getAsJsonObject());

        // Update and save current PIDs
        positionHandler.setPIDFromJson(pidsNew);
        FileWorkers.saveJsonObject(pidsNew, Main.settings.get("pid_file").getAsString());

        // Enable or disable video stream checkbox
        positionHandler.osdHandler.streamEnabled = query.get("video").equals("true");

        // Redirect to the home page
        resp.redirect("/");
    }

    /**
     * Starts stabilizing or landing
     */
    @Route("/hold_land")
    public void holdLand(Response resp) {
        Query query = resp.getRequest().getQuery();
        // Set stabilization flag in PositionHandler class
        positionHandler.stabEnabled = query.get("hold").equals("true");

        // Set landing flag in PositionHandler class
        positionHandler.landingEnabled = query.get("land").equals("true");

        // Redirect to the home page
        resp.redirect("/");
    }

    /**
     * Stops stabilization and shutdown all
     */
    @Route("/abort")
    public void abort(Response resp) {
        logger.warn("Aborting.");
        // Redirect to the home page with aborted flag provided
        aborted = true;
        resp.redirect("/");
        try {
            // Stop video stream and OpenCV handler
            osdHandler.stop();
            openCVHandler.stop();

            // Tells the drone to stop DDC stabilization
            positionHandler.TransmitPosition(0);

            // Close UDP and Serial ports
            udpHandler.closeUDP();
            serialHandler.closePorts();
        } catch (Exception ignored) { }
        // Call system shutdown
        new Timer(1000, e -> System.exit(0)).start();
    }

    /**
     * Starts Flak server
     */
    public void start() {
        try {
            logger.info("Trying to start the server...");
            // Create Flak server with provided port and ip
            app = Flak.createHttpApp(serverPort);
            app.getServer().setHostName(hostName);

            // Add static directory (for resources)
            Path dir = Paths.get(Main.settings.get("web_resources_folder").getAsString());
            new FlakResourceImpl(app).servePath("/",
                    dir.toString(),
                    getClass().getClassLoader(),
                    false);

            // Start the server from this class
            app.scan(this);
            app.start();
            logger.info("The server is running on " + hostName + ":" + serverPort);
        } catch (Exception e) {
            logger.error("Error starting the server!", e);
            System.exit(1);
        }
    }
}
