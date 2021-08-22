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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

public class SettingsHandler {
    private final Logger logger = Logger.getLogger(this.getClass().getSimpleName());
    private final SettingsContainer settingsContainer;
    private final JsonObject jsonSettings;

    public SettingsHandler(SettingsContainer settingsContainer, JsonObject jsonSettings) {
        this.settingsContainer = settingsContainer;
        this.jsonSettings = jsonSettings;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void parseSettings() {
        try {
            logger.info("Parsing settings from JSON");

            // Marker Size
            settingsContainer.markerSize = jsonSettings.get("marker_size").getAsFloat();
            if (settingsContainer.markerSize <= 0.0)
                exitWithError("Invalid marker size");

            // Default camera exposure
            settingsContainer.maxExposure = jsonSettings.get("max_exposure").getAsDouble();

            // Landing altitude
            settingsContainer.motorsTurnOffHeight = jsonSettings.get("motors_turn_off_height").getAsDouble();
            if (settingsContainer.motorsTurnOffHeight < 0)
                exitWithError("Invalid landing (motors turn off) height");

            // Landing allowed
            settingsContainer.landingAllowed = jsonSettings.get("landing_allowed").getAsBoolean();

            // Disable on-board Liberty Way sequence (only optical stabilization)
            settingsContainer.onlyOpticalStabilization = jsonSettings.get("only_optical_stabilization").getAsBoolean();

            // The maximum height at which the marker is accepted for optical stabilization
            settingsContainer.maxMarkerHeight = jsonSettings.get("max_marker_height").getAsInt();
            if (settingsContainer.maxMarkerHeight < settingsContainer.motorsTurnOffHeight)
                exitWithError("The maximum height of the marker (drone) is less than the landing altitude");

            // PID file
            settingsContainer.pidFile = jsonSettings.get("pid_file").getAsString();
            if (!new File(settingsContainer.pidFile).exists())
                exitWithError("PID file doesn't exists");

            // Camera matrix file
            settingsContainer.cameraMatrixFile = jsonSettings.get("camera_matrix_file").getAsString();
            if (!new File(settingsContainer.cameraMatrixFile).exists())
                exitWithError("Camera matrix file doesn't exists");

            // Camera distortions file
            settingsContainer.cameraDistortionsFile = jsonSettings.get("camera_distortions_file").getAsString();
            if (!new File(settingsContainer.cameraDistortionsFile).exists())
                exitWithError("Camera distortions file doesn't exists");

            // Watermark file
            settingsContainer.watermarkFile = jsonSettings.get("watermark_file").getAsString();
            if (!new File(settingsContainer.watermarkFile).exists())
                exitWithError("Watermark file doesn't exists");

            // Web resources folder
            settingsContainer.webResourcesFolder = jsonSettings.get("web_resources_folder").getAsString();
            if (!new File(settingsContainer.webResourcesFolder).exists())
                exitWithError("Web resources folder doesn't exists");

            // Web templates folder
            settingsContainer.webTemplatesFolder = jsonSettings.get("web_templates_folder").getAsString();
            if (!new File(settingsContainer.webTemplatesFolder).exists())
                exitWithError("Web templates folder doesn't exists");

            // Blackbox folder
            settingsContainer.blackboxFolder = jsonSettings.get("blackbox_folder").getAsString();
            if (!new File(settingsContainer.blackboxFolder).exists())
                new File(settingsContainer.blackboxFolder).mkdirs();

            // Frame width
            settingsContainer.frameWidth = jsonSettings.get("frame_width").getAsInt();
            if (settingsContainer.frameWidth <= 0)
                exitWithError("Invalid frame width");

            // Frame height
            settingsContainer.frameHeight = jsonSettings.get("frame_height").getAsInt();
            if (settingsContainer.frameHeight <= 0)
                exitWithError("Invalid frame height");

            // Disable auto exposure
            settingsContainer.disableAutoExposure = jsonSettings.get("disable_auto_exposure").getAsBoolean();

            // Disable auto white balance
            settingsContainer.disableAutoWB = jsonSettings.get("disable_auto_wb").getAsBoolean();

            // Disable auto focus
            settingsContainer.disableAutoFocus = jsonSettings.get("disable_auto_focus").getAsBoolean();

            // Default server host
            settingsContainer.defaultServerHost = jsonSettings.get("default_server_host").getAsString();

            // Default server port
            settingsContainer.defaultServerPort = jsonSettings.get("default_server_port").getAsInt();

            // Default video port
            settingsContainer.defaultVideoPort = jsonSettings.get("default_video_port").getAsInt();

            // Is video stream enabled by default
            settingsContainer.videoStreamEnabledByDefault =
                    jsonSettings.get("video_stream_enabled_by_default").getAsBoolean();

            // Is blackbox enabled
            settingsContainer.blackboxEnabled = jsonSettings.get("blackbox_enabled").getAsBoolean();

            // Serial reconnect time
            settingsContainer.serialReconnectTime = jsonSettings.get("serial_reconnect_time").getAsInt();

            // UDP port timeout
            settingsContainer.udpTimeout = jsonSettings.get("udp_timeout").getAsInt();

            // Telemetry lost time
            settingsContainer.telemetryLostTime = jsonSettings.get("telemetry_lost_time").getAsInt();

            // Platform lost time
            settingsContainer.platformLostTime = jsonSettings.get("platform_lost_time").getAsInt();

            // Platform light enable threshold
            settingsContainer.platformLightEnableThreshold =
                    jsonSettings.get("platform_light_enable_threshold").getAsInt();

            // Platform light disable threshold
            settingsContainer.platformLightDisableThreshold =
                    jsonSettings.get("platform_light_disable_threshold").getAsInt();

            // Platform loop timer
            settingsContainer.platformLoopTimer = jsonSettings.get("platform_loop_timer").getAsInt();

            // FPS measure period
            settingsContainer.fpsMeasurePeriod = jsonSettings.get("fps_measure_period").getAsInt();

            // Adaptive threshold constant
            settingsContainer.adaptiveThreshConstant = jsonSettings.get("adaptive_thresh_constant").getAsInt();

            // Aruco dictionary
            settingsContainer.arucoDictionary = jsonSettings.get("aruco_dictionary").getAsShort();
            if (settingsContainer.arucoDictionary < 0)
                exitWithError("Invalid ARUco dictionary");

            // Allowed marker ids
            JsonArray jsonAllowedIDs = jsonSettings.get("allowed_ids").getAsJsonArray();
            if (Objects.requireNonNull(jsonAllowedIDs).size() < 1)
                exitWithError("Invalid array of allowed marker IDs");

            settingsContainer.allowedIDs = new ArrayList<>();
            for (int i = 0; i < Objects.requireNonNull(jsonAllowedIDs).size(); i++){
                settingsContainer.allowedIDs.add(jsonAllowedIDs.get(i).getAsInt());
            }

            // Input filter factor
            settingsContainer.inputFilter = jsonSettings.get("input_filter").getAsDouble();
            if (settingsContainer.inputFilter < 0.0 || settingsContainer.inputFilter > 1.0)
                exitWithError("Invalid input filter factor");

            // Setpoint alignment factor
            settingsContainer.setpointAlignmentFactor = jsonSettings.get("setpoint_alignment_factor").getAsDouble();
            if (settingsContainer.setpointAlignmentFactor < 0.0 || settingsContainer.setpointAlignmentFactor > 1.0)
                exitWithError("Invalid setpoint alignment factor");

            // Allowed lost frames
            settingsContainer.allowedLostFrames = jsonSettings.get("allowed_lost_frames").getAsInt();

            // Landing decrement
            settingsContainer.landingDecrement = jsonSettings.get("landing_decrement").getAsDouble();
            if (settingsContainer.landingDecrement < 0)
                exitWithError("Invalid landing decrement");

            // Allowed landing range on X and Y axes
            settingsContainer.allowedLandingRangeXY = jsonSettings.get("allowed_landing_range_xy").getAsDouble();

            // Allowed range on Yaw axis
            settingsContainer.allowedLandingRangeYaw = jsonSettings.get("allowed_landing_range_yaw").getAsDouble();

            // Minimum number of satellites to begin Liberty-Way sequence
            settingsContainer.minSatellitesNumStart = jsonSettings.get("min_satellites_num_start").getAsShort();
            if (settingsContainer.minSatellitesNumStart < 0)
                exitWithError("Invalid minimum start satellites number");

            // Minimum number of satellites to continue Liberty-Way sequence
            settingsContainer.minSatellitesNum = jsonSettings.get("min_satellites_num").getAsShort();
            if (settingsContainer.minSatellitesNum < 0)
                exitWithError("Invalid minimum satellites number");

            // Setpoint X
            settingsContainer.setpointX = jsonSettings.get("setpoint_x").getAsDouble();

            // Setpoint Y
            settingsContainer.setpointY = jsonSettings.get("setpoint_y").getAsDouble();

            // Setpoint Yaw
            settingsContainer.setpointYaw = jsonSettings.get("setpoint_yaw").getAsDouble();

            // Data suffix 1 (Liberty-Link packet ending)
            settingsContainer.droneDataSuffix1 = jsonSettings.get("drone_data_suffix_1").getAsByte();

            // Data suffix 2 (Liberty-Link packet ending)
            settingsContainer.droneDataSuffix2 = jsonSettings.get("drone_data_suffix_2").getAsByte();

            // Data suffix 1 (Eitude packet ending)
            settingsContainer.platformDataSuffix1 = jsonSettings.get("platform_data_suffix_1").getAsByte();

            // Data suffix 2 (Eitude packet ending)
            settingsContainer.platformDataSuffix2 = jsonSettings.get("platform_data_suffix_2").getAsByte();

            // Push OSD and Video frame after openCV frames
            settingsContainer.pushOSDAfterFrames = jsonSettings.get("push_osd_after_frames").getAsShort();
            if (settingsContainer.pushOSDAfterFrames < 0)
                exitWithError("Invalid \"push OSD after frames\" number");

            // Radius of a current planet that the project operates on
            settingsContainer.planetRadius = jsonSettings.get("planet_radius").getAsDouble();
            if (settingsContainer.planetRadius <= 0)
                exitWithError("Invalid planet radius");

            // Speed filter factor
            settingsContainer.speedFilter = jsonSettings.get("speed_filter").getAsDouble();
            if (settingsContainer.speedFilter < 0.0 || settingsContainer.speedFilter > 1.0)
                exitWithError("Invalid speed filter factor");

            // How many pascals will be added to pressure waypoint
            settingsContainer.pressureTermAbovePlatform = jsonSettings.get("pressure_term_above_platform").getAsInt();
            if (settingsContainer.pressureTermAbovePlatform < -200
                    || settingsContainer.pressureTermAbovePlatform > 200)
                exitWithError("Invalid pressure term");

            // How many cycles will the IDLE command be sent (instead of the waypoint command)
            settingsContainer.sendIdleCyclesNum = jsonSettings.get("send_idle_cycles_num").getAsShort();
            if (settingsContainer.sendIdleCyclesNum < 0)
                exitWithError("Invalid IDLE cycles number");

            // Is telemetry necessary for liberty-way sequence?
            settingsContainer.isTelemetryNecessary = jsonSettings.get("is_telemetry_necessary").getAsBoolean();

            // Maximum platform speed
            settingsContainer.maxPlatformSpeed = jsonSettings.get("max_platform_speed").getAsInt();
            if (settingsContainer.maxPlatformSpeed < 0)
                exitWithError("Wrong maximum platform speed");

            // Whether to send the IDLE command when the system is in WAIT mode (0)
            settingsContainer.sendIDLEInWAITMode = jsonSettings.get("send_idle_in_wait_mode").getAsBoolean();

            // Is GPS Prediction allowed
            settingsContainer.isGPSPredictionAllowed = jsonSettings.get("is_gps_prediction_allowed").getAsBoolean();

            // Stop GPS predictions if distance between drone and platform is less that this threshold
            settingsContainer.stopPredictionOnDistance = jsonSettings.get("stop_prediction_on_distance").getAsInt();
            if (settingsContainer.stopPredictionOnDistance < 0)
                exitWithError("Wrong prediction stop distance");

            // Is hardware compass on platforms enables
            settingsContainer.platformHardwareCompass = jsonSettings.get("platform_hardware_compass").getAsBoolean();

            // Is FPS logging enables
            settingsContainer.logFPS = jsonSettings.get("log_fps").getAsBoolean();

            // Is API requests logging enables
            settingsContainer.logAPIRequests = jsonSettings.get("log_api_requests").getAsBoolean();

            // If all checks are passed
            logger.info("Basic checks passed. Settings loaded");

        } catch (Exception e) {
            // Print error message
            logger.error("Error parsing settings!", e);
            // Exit because no correct settings provided
            System.exit(1);
        }
    }

    private void exitWithError(String errorMessage) {
        // Print error message
        logger.error("Error parsing settings! " + errorMessage);

        // Exit because no correct settings provided
        System.exit(1);
    }
}
