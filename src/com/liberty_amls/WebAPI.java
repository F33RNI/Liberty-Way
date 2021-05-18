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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import flak.Response;
import org.apache.log4j.Logger;
import org.opencv.core.Core;
import org.opencv.videoio.VideoCapture;

import javax.swing.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;

public class WebAPI {
    private final Logger logger = Logger.getLogger(this.getClass().getSimpleName());
    private final SettingsContainer settingsContainer;
    private final DecimalFormat decimalFormat = new DecimalFormat("#.#");
    private final DecimalFormat decimalFormatInt = new DecimalFormat("#.#");
    private final String hostName;
    private final int videoPort;
    private boolean aborted = false, controllerRunning = false;
    private boolean progressTick = false;

    private BlackboxHandler blackboxHandler;
    private OpenCVHandler openCVHandler;
    private OSDHandler osdHandler;
    private UDPHandler udpHandler;
    private SerialHandler serialHandler;
    private PositionHandler positionHandler;
    private PlatformHandler platformHandler;
    private TelemetryHandler telemetryHandler;
    private TelemetryContainer telemetryContainer;
    private PlatformContainer platformContainer;
    private PositionContainer positionContainer;
    private GPSEstimationContainer gpsEstimationContainer;
    private GPSEstimationHandler gpsEstimationHandler;

    /**
     * This class provides a web API. The ability to send and receive data using POST JSON requests
     * @param hostName main IP
     * @param videoPort video stream port
     */
    public WebAPI(String hostName, int videoPort, SettingsContainer settingsContainer) {
        this.videoPort = videoPort;
        this.hostName = hostName;
        this.settingsContainer = settingsContainer;
    }

    /**
     * Processes a request to the API
     * @param response Flak response
     * @return Flak response
     */
    public Response newRequest(Response response) throws IOException {
        JsonObject apiResponse = new JsonObject();
        if (response.getRequest().getHeader("content-type").equals("application/json")) {

            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            for (int length; (length = response.getRequest().getInputStream().read(buffer)) != -1; ) {
                result.write(buffer, 0, length);
            }
            JsonObject request = new Gson().fromJson(result.toString(), JsonObject.class);

            logger.info("New API request: " + request.toString());

            try {
                String action = request.get("action").getAsString().toLowerCase();

                switch (action) {
                    case ("setup"):
                        // Initialize all modules and start controller
                        if (!controllerRunning) {
                            startController(request);
                            response.setStatus(200);
                            apiResponse.add("status", new JsonPrimitive("ok"));

                        } else {
                            // The controller is already running
                            returnError(response, apiResponse, "The controller is already running!", 418);
                        }
                        break;

                    case ("abort"):
                        // Abort and exit
                        if (controllerRunning) {
                            // Close all modules and exit (abort after initialization)
                            abortController();
                        } else {
                            // Exit without closing any modules (abort before initialization)
                            abortSetup();
                        }
                        response.setStatus(200);
                        apiResponse.add("status", new JsonPrimitive("ok"));
                        break;

                    case ("telemetry"):
                        // Request telemetry data
                        if (controllerRunning) {
                            apiResponse.add("status", new JsonPrimitive("ok"));
                            apiResponse.add("telemetry", fillTelemetry());
                            response.setStatus(200);

                        } else {
                            // The controller is not running
                            returnError(response, apiResponse, "The controller is not running!", 418);
                        }
                        break;

                    case ("toggle_stream"):
                        // Start or stop JPEG video stream with OSD
                        if (controllerRunning) {
                            if (osdHandler.isStreamEnabled()) {
                                osdHandler.disableStreamAndOSD();
                                apiResponse.add("video", new JsonPrimitive("disabled"));
                                logger.info("Video stream disabled");
                            } else {
                                osdHandler.enableStreamAndOSD();
                                apiResponse.add("video", new JsonPrimitive("enabled"));
                                logger.info("Video stream enabled");
                            }
                            response.setStatus(200);
                            apiResponse.add("status", new JsonPrimitive("ok"));
                        } else {
                            // The controller is not running
                            returnError(response, apiResponse, "The controller is not running!", 418);
                        }
                        break;

                    case ("check_stream"):
                        // Check if JPEG video stream with OSD enabled
                        if (controllerRunning) {
                            apiResponse.add("video",
                                    new JsonPrimitive(osdHandler.isStreamEnabled() ? "enabled" : "disabled"));
                            response.setStatus(200);
                            apiResponse.add("status", new JsonPrimitive("ok"));
                        } else {
                            // The controller is not running
                            returnError(response, apiResponse, "The controller is not running!", 418);
                        }
                        break;

                    case ("execute"):
                        // Execute main sequence
                        if (controllerRunning) {
                            logger.info("Starting Liberty-Way sequence");
                            positionHandler.setLibertyWayEnabled(true);
                            apiResponse.add("status", new JsonPrimitive("ok"));
                            response.setStatus(200);
                        } else {
                            // The controller is not running
                            returnError(response, apiResponse, "The controller is not running!", 418);
                        }
                        break;

                    default:
                        // Wrong action request
                        returnError(response, apiResponse, "I'm a teapot!", 418);
                        break;
                }
            } catch (Exception e) {
                logger.error("An error occurred during an API request!", e);
                // Unknown action
                returnError(response, apiResponse, e.toString(), 500);
            }
        } else {
            // Wrong (not JSON) content-type
            returnError(response, apiResponse, "I'm a JSON teapot!", 418);
        }
        response.getOutputStream().write(apiResponse.toString().getBytes());
        response.getOutputStream().close();
        return response;
    }

    /**
     * @return true if the application is aborted
     */
    public boolean isAborted() {
        return aborted;
    }

    /**
     * @return true if core modules have been configured
     */
    public boolean isControllerRunning() {
        return controllerRunning;
    }

    /**
     * Sets error status with message to API response
     */
    private void returnError(Response response, JsonObject apiResponse, String errorMessage, int status) {
        response.setStatus(status);
        apiResponse.add("status", new JsonPrimitive("error"));
        apiResponse.add("message", new JsonPrimitive(errorMessage));
        logger.warn("Error during API request! " + errorMessage + " The request was ignored");
    }

    /**
     * Initializes all core modules
     */
    @SuppressWarnings("StatementWithEmptyBody")
    private void startController(JsonObject setupData) throws UnknownHostException {
        // Log all data submitted from the form
        logger.info("Starting the controller");
        logger.info("Platform Controller Port: " + setupData.get("platform_port").getAsString());
        if (setupData.get("platform_port").getAsString().length() > 0)
            logger.info("Platform Controller Port baudrate: " + setupData.get("platform_baudrate").getAsString());
        logger.info("Liberty-Link Port: " + setupData.get("link_port").getAsString());
        if (setupData.get("link_port").getAsString().length() > 0)
            logger.info("Liberty-Link Port baudrate: " + setupData.get("link_baudrate").getAsString());
        logger.info("UDP Ip/Port: " + setupData.get("udp_ip_port").getAsString());
        logger.info("Camera ID: " + setupData.get("camera_id").getAsString());

        // Load native library (from java-library-path)
        logger.info("Loading OpenCV Native Library");
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // Create SerialHandler class for serial communication
        serialHandler = new SerialHandler(setupData.get("platform_port").getAsString(),
                setupData.get("platform_port").getAsString().length() > 0 ?
                        setupData.get("platform_baudrate").getAsString() : "",
                setupData.get("link_port").getAsString(),
                setupData.get("link_port").getAsString().length() > 0 ?
                        setupData.get("link_baudrate").getAsString() : "",
                settingsContainer.serialReconnectTime);
        serialHandler.openPorts();

        // Create UDPHandler class for UDP communication
        udpHandler = new UDPHandler(setupData.get("udp_ip_port").getAsString());
        udpHandler.openUDP();
        if (udpHandler.isUdpPortOpened()) {
            // Create and start a new thread with the normal priority for the UDP handler (async reader)
            Thread udpThread = new Thread(udpHandler);
            udpThread.setPriority(Thread.NORM_PRIORITY);
            udpThread.start();
        }

        // Create PositionContainer class for store current position
        positionContainer = new PositionContainer();

        // Create PlatformContainer class for store platform data
        platformContainer = new PlatformContainer(settingsContainer.defaultExposure);

        // Create TelemetryContainer class for store telemetry data
        telemetryContainer = new TelemetryContainer();

        // Create VideoCapture class
        VideoCapture videoCapture = new VideoCapture();

        // Create PlatformHandler class for integrating with platform
        platformHandler = new PlatformHandler(platformContainer, positionContainer,
                serialHandler, videoCapture, settingsContainer);
        // Create and start a new thread for the platformHandler if platform port is open
        if (serialHandler.isPlatformPortOpened()) {
            Thread platformThread = new Thread(platformHandler);
            platformThread.start();
        } else
            logger.warn("No communication with the platform!");

        // Create TelemetryHandler class for read the telemetry data
        telemetryHandler = new TelemetryHandler(telemetryContainer, serialHandler, udpHandler,
                settingsContainer.telemetryLostTime, settingsContainer.dataSuffix1, settingsContainer.dataSuffix2);

        // Create and start a new thread for the platformHandler if Liberty-Link port is open
        if (serialHandler.isLinkPortOpened() || udpHandler.isUdpPortOpened()) {
            Thread telemetryThread = new Thread(telemetryHandler);
            telemetryThread.setPriority(Thread.NORM_PRIORITY);
            telemetryThread.start();
        } else
            logger.warn("No Liberty-Link port! Telemetry data cannot be read!");

        // Create OSDHandler and VideoStream classes
        osdHandler = new OSDHandler(new VideoStream(InetAddress.getByName(hostName), videoPort,
                settingsContainer.frameWidth,
                settingsContainer.frameHeight),
                positionContainer, platformContainer, settingsContainer.watermarkFile);

        // Create BlackboxHandler class for logging all events and position
        blackboxHandler = new BlackboxHandler(positionContainer,
                platformContainer,
                settingsContainer.blackboxFolder);
        // Create and start a new thread with the normal priority for the blackbox
        Thread blackboxThread = new Thread(blackboxHandler);
        blackboxThread.setPriority(Thread.NORM_PRIORITY);
        blackboxThread.start();

        // Create GPSEstimationContainer class for storing GPS coordinates
        gpsEstimationContainer = new GPSEstimationContainer();

        // Create GPSEstimationHandler class for integrating with the platform
        gpsEstimationHandler = new GPSEstimationHandler(gpsEstimationContainer, telemetryContainer);

        // Create PositionHandler class for to handle the current position
        positionHandler = new PositionHandler(serialHandler, udpHandler, positionContainer, platformContainer,
                telemetryContainer, blackboxHandler, settingsContainer, gpsEstimationContainer, gpsEstimationHandler);

        // Set coefficients for MiniPID in PositionHandler class
        positionHandler.loadPIDFromFile();

        // Create OpenCVHandler class for find marker and estimate its position
        openCVHandler = new OpenCVHandler(Integer.parseInt(setupData.get("camera_id").getAsString()),
                videoCapture,
                positionHandler,
                positionContainer,
                platformHandler,
                osdHandler,
                settingsContainer);
        openCVHandler.start();

        // Create and start a new thread with the highest priority for opencv handler
        Thread openCVThread = new Thread(openCVHandler);
        openCVThread.setPriority(Thread.MAX_PRIORITY);
        openCVThread.start();

        // Wait for the first frame from OpenCVHandler
        while (openCVHandler.isFrameEmpty()) ;

        // Create and start a new thread with the lowest priority for the video stream
        Thread osdThread = new Thread(osdHandler);
        osdThread.setPriority(Thread.MIN_PRIORITY);
        osdThread.start();

        // Enable video stream
        if (settingsContainer.videoStreamEnabledByDefault)
            osdHandler.enableStreamAndOSD();

        // Enable or disable blackbox
        blackboxHandler.setBlackboxEnabled(settingsContainer.blackboxEnabled);

        controllerRunning = true;

        logger.info("Controller startup is complete. Please visit '/' page");
    }

    /**
     * Aborts controller setup
     */
    private void abortSetup() {
        logger.warn("Aborting");
        // Redirect to the home page with aborted flag provided
        aborted = true;

        // Call system shutdown
        new Timer(1000, e -> System.exit(0)).start();
    }

    /**
     * Aborts Liberty-Way controller
     */
    private void abortController() {
        logger.warn("Aborting");
        // Redirect to the home page with aborted flag provided
        aborted = true;
        try {
            // Stop all the handler
            blackboxHandler.stop();
            telemetryHandler.stop();
            platformHandler.stop();
            osdHandler.stop();
            // Tells the drone to stop stabilization
            positionHandler.setLibertyWayEnabled(false);
            // Close OpenCV handler
            openCVHandler.stop();
            // Close UDP and Serial ports
            udpHandler.closeUDP();
            serialHandler.closePorts();
        } catch (Exception ignored) { }
        // Call system shutdown
        new Timer(1000, e -> System.exit(0)).start();
    }

    /**
     * Adds all telemetry data to JSON
     * @return JsonObject with telemetry data
     */
    private JsonObject fillTelemetry() {
        JsonObject telemetry = new JsonObject();

        // Timeline
        telemetry.add("progress", new JsonPrimitive(decimalFormat.format(calculateProgress())));

        // Current system status
        telemetry.add("status",
                new JsonPrimitive(positionContainer.getStatusString()));

        // Drone telemetry data
        telemetry.add("drone_packets",
                new JsonPrimitive(decimalFormat.format(telemetryContainer.packetsNumber)));
        telemetry.add("flight_mode",
                new JsonPrimitive(decimalFormatInt.format(telemetryContainer.flightMode)));
        telemetry.add("drone_voltage",
                new JsonPrimitive(decimalFormatInt.format(telemetryContainer.batteryVoltage)));
        telemetry.add("drone_altitude",
                new JsonPrimitive(decimalFormatInt.format(telemetryContainer.altitude)));
        telemetry.add("drone_satellites",
                new JsonPrimitive(decimalFormatInt.format(telemetryContainer.satellitesNum)));
        telemetry.add("drone_lat",
                new JsonPrimitive(String.valueOf(telemetryContainer.gpsLatDouble)));
        telemetry.add("drone_lon",
                new JsonPrimitive(String.valueOf(telemetryContainer.gpsLonDouble)));

        // Platform telemetry data
        telemetry.add("platform_packets",
                new JsonPrimitive(decimalFormat.format(platformContainer.packetsNumber)));
        telemetry.add("platform_speed",
                new JsonPrimitive(decimalFormat.format(platformContainer.speed)));
        telemetry.add("platform_pressure",
                new JsonPrimitive(decimalFormatInt.format(platformContainer.pressure)));
        telemetry.add("platform_satellites",
                new JsonPrimitive(String.valueOf(platformContainer.satellitesNum)));
        telemetry.add("platform_lat",
                new JsonPrimitive(String.valueOf(platformContainer.gpsLatDouble)));
        telemetry.add("platform_lon",
                new JsonPrimitive(String.valueOf(platformContainer.gpsLonDouble)));

        return telemetry;
    }

    /**
     * Calculates progress for timeline
     * @return total progress 0-100%
     */
    private double calculateProgress() {
        progressTick = !progressTick;
        if (positionContainer.status == 1 || positionContainer.status == 3) {
            // Optical stabilization
            if (progressTick)
                return (100.0 / 6.0) * 5;
            else
                return (100.0 / 6.0) * 4;
        } else if (positionContainer.status == 2) {
            // Landing
            if (positionContainer.z <= positionContainer.entryZ
                    && positionContainer.z >= settingsContainer.landingAlt) {
                return (100.0 / 6.0) *
                        mapDouble(positionContainer.z,
                                settingsContainer.landingAlt,
                                positionContainer.entryZ,
                        6.0,
                        5.0);
            }
            else {
                return (100.0 / 6.0) * 5;
            }

        } else if (positionContainer.status == 7) {
            // Landed
            return 100.0;
        } else if (positionContainer.status == 5 || positionContainer.status == 6) {
            // TKOF or WAYP mode
            if (telemetryContainer.linkWaypointStep == 1) {
                // Pre-starting the motors
                return (100.0 / 6.0) * 0.5;
            }
            else if (telemetryContainer.linkWaypointStep == 2) {
                // Waiting for taking off
                if (progressTick)
                    return (100.0 / 6.0) * 1;
                else
                    return (100.0 / 6.0) * 0.5;
            }
            else if (telemetryContainer.linkWaypointStep == 3) {
                // Ascending
                if (progressTick)
                    return (100.0 / 6.0) * 2;
                else
                    return (100.0 / 6.0) * 1;
            }
            else if (telemetryContainer.linkWaypointStep == 4 || telemetryContainer.linkWaypointStep == 5) {
                // GPS Flight
                if (progressTick)
                    return (100.0 / 6.0) * 3;
                else
                    return (100.0 / 6.0) * 2;
            }
            else if (telemetryContainer.linkWaypointStep == 6) {
                // GPS and altitude hold
                if (progressTick)
                    return (100.0 / 6.0) * 4;
                else
                    return (100.0 / 6.0) * 3;
            }
            else
                return 0;
        }
        return 0;
    }

    /**
     * Analogue of Arduino's map function
     */
    private double mapDouble(double value, double in_min, double in_max, double out_min, double out_max)
    {
        return (value - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
    }
}
