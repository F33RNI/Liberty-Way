/*
 * Copyright (C) 2021 Frey Hertz (Pavel Neshumov), Liberty-Way Landing System Project
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
                exitWithError("Wrong marker size");

            // Default camera exposure
            settingsContainer.defaultExposure = jsonSettings.get("default_exposure").getAsDouble();

            // Landing altitude
            settingsContainer.landingAlt = jsonSettings.get("landing_alt").getAsDouble();
            if (settingsContainer.landingAlt < 0)
                exitWithError("Wrong landing altitude");

            // Landing allowed
            settingsContainer.landingAllowed = jsonSettings.get("landing_allowed").getAsBoolean();

            // Disable on-board Liberty Way sequence (only optical stabilization)
            settingsContainer.onlyOpticalStabilization = jsonSettings.get("only_optical_stabilization").getAsBoolean();

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
                exitWithError("Wrong frame width");

            // Frame height
            settingsContainer.frameHeight = jsonSettings.get("frame_height").getAsInt();
            if (settingsContainer.frameHeight <= 0)
                exitWithError("Wrong frame height");

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

            // Platform reply timeout
            settingsContainer.platformReplyTimeout = jsonSettings.get("platform_reply_timeout").getAsInt();

            // Platform loop timer
            settingsContainer.platformLoopTimer = jsonSettings.get("platform_loop_timer").getAsInt();

            // FPS measure period
            settingsContainer.fpsMeasurePeriod = jsonSettings.get("fps_measure_period").getAsInt();

            // Adaptive threshold constant
            settingsContainer.adaptiveThreshConstant = jsonSettings.get("adaptive_thresh_constant").getAsInt();

            // Aruco dictionary
            settingsContainer.arucoDictionary = jsonSettings.get("aruco_dictionary").getAsShort();
            if (settingsContainer.arucoDictionary < 0)
                exitWithError("Wrong ARUco dictionary");

            // Allowed marker ids
            JsonArray jsonAllowedIDs = jsonSettings.get("allowed_ids").getAsJsonArray();
            if (Objects.requireNonNull(jsonAllowedIDs).size() < 1)
                exitWithError("Wrong array of allowed marker IDs");

            settingsContainer.allowedIDs = new ArrayList<>();
            for (int i = 0; i < Objects.requireNonNull(jsonAllowedIDs).size(); i++){
                settingsContainer.allowedIDs.add(jsonAllowedIDs.get(i).getAsInt());
            }

            // Input filter factor
            settingsContainer.inputFilter = jsonSettings.get("input_filter").getAsDouble();
            if (settingsContainer.inputFilter < 0.0 || settingsContainer.inputFilter > 1.0)
                exitWithError("Wrong input filter factor");

            // Setpoint alignment factor
            settingsContainer.setpointAlignmentFactor = jsonSettings.get("setpoint_alignment_factor").getAsDouble();
            if (settingsContainer.setpointAlignmentFactor < 0.0 || settingsContainer.setpointAlignmentFactor > 1.0)
                exitWithError("Wrong setpoint alignment factor");

            // Allowed lost frames
            settingsContainer.allowedLostFrames = jsonSettings.get("allowed_lost_frames").getAsInt();

            // Landing decrement
            settingsContainer.landingDecrement = jsonSettings.get("landing_decrement").getAsDouble();
            if (settingsContainer.landingDecrement < 0)
                exitWithError("Wrong landing decrement");

            // Allowed landing range on X and Y axes
            settingsContainer.allowedLandingRangeXY = jsonSettings.get("allowed_landing_range_xy").getAsDouble();

            // Allowed range on Yaw axis
            settingsContainer.allowedLandingRangeYaw = jsonSettings.get("allowed_landing_range_yaw").getAsDouble();

            // Setpoint X
            settingsContainer.setpointX = jsonSettings.get("setpoint_x").getAsDouble();

            // Setpoint Y
            settingsContainer.setpointY = jsonSettings.get("setpoint_y").getAsDouble();

            // Setpoint Yaw
            settingsContainer.setpointYaw = jsonSettings.get("setpoint_yaw").getAsDouble();

            // Data suffix 1 (Liberty-Link packet ending)
            settingsContainer.dataSuffix1 = jsonSettings.get("data_suffix_1").getAsString().getBytes()[0];

            // Data suffix 2 (Liberty-Link packet ending)
            settingsContainer.dataSuffix2 = jsonSettings.get("data_suffix_2").getAsString().getBytes()[0];

            // Push OSD and Video frame after openCV frames
            settingsContainer.pushOSDAfterFrames = jsonSettings.get("push_osd_after_frames").getAsShort();
            if (settingsContainer.pushOSDAfterFrames < 0)
                exitWithError("Wrong \"push OSD after frames\" number");

            // Radius of a current planet that the project operates on
            settingsContainer.planetRadius = jsonSettings.get("planet_radius").getAsInt();
            if (settingsContainer.planetRadius == 0)
                exitWithError("Invalid planet radius");

            // Distance in which the drone considered far enough to use K = 1
            settingsContainer.notAcceptableDistance = jsonSettings.get("not_acceptable_distance").getAsInt();

            // Debug option for whether to allow prediction of GPS coordinates or not
            settingsContainer.allowPrediction = jsonSettings.get("allow_prediction").getAsBoolean();

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
