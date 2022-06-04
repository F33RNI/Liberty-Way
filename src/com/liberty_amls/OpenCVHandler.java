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

import org.apache.log4j.Logger;
import org.opencv.aruco.Aruco;
import org.opencv.aruco.DetectorParameters;
import org.opencv.aruco.Dictionary;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.abs;
import static java.lang.Math.toDegrees;

public class OpenCVHandler implements Runnable {
    private final Logger logger = Logger.getLogger(this.getClass().getSimpleName());
    private final SettingsContainer settingsContainer;
    private final PositionHandler positionHandler;
    private final PositionContainer positionContainer;
    private final TelemetryContainer telemetryContainer;
    private final PlatformContainer platformContainer;
    private final OSDHandler osdHandler;
    private final VideoCapture videoCapture;
    private final int cameraID;
    private Dictionary dictionary;
    private boolean openCVRunning;
    private int framesCount;
    private long timeStart;
    private final DecimalFormat decimalFormat = new DecimalFormat("#.#");
    private Mat cameraMatrix, cameraDistortions;
    private final Mat frame = new Mat(), gray = new Mat();
    private int pushOSDAfterFrames, osdFramesCounter = 0;

    /**
     * This class reads frame from camera, estimate ARUco marker position
     * and provides it to the other classes
     */
    public OpenCVHandler(int cameraID,
                         VideoCapture videoCapture,
                         PositionHandler positionHandler,
                         PositionContainer positionContainer,
                         TelemetryContainer telemetryContainer,
                         PlatformContainer platformContainer,
                         OSDHandler osdHandler,
                         SettingsContainer settingsContainer) {
        this.cameraID = cameraID;
        this.videoCapture = videoCapture;
        this.positionHandler = positionHandler;
        this.positionContainer = positionContainer;
        this.telemetryContainer = telemetryContainer;
        this.platformContainer = platformContainer;
        this.osdHandler = osdHandler;
        this.settingsContainer = settingsContainer;
        framesCount = 0;
    }

    /**
     * Loads settings and opens the camera
     * @return true if camera opened successfully false if not
     */
    public boolean start() {
        try {
            // Load camera corrections from jsons
            cameraMatrix = FileWorkers.loadCameraMatrix(settingsContainer.cameraMatrixFile);
            cameraDistortions = FileWorkers.loadCameraDistortions(settingsContainer.cameraDistortionsFile);

            // Load ARUco dictionary from settings
            dictionary = Aruco.getPredefinedDictionary(settingsContainer.arucoDictionary);

            // Set after how many frame the frame will be pushed to the OSD class
            pushOSDAfterFrames = settingsContainer.pushOSDAfterFrames;

            // Load settings
            positionContainer.frameSetpoint = new Point(settingsContainer.frameWidth / 2.0,
                    settingsContainer.frameHeight / 2.0);

            // Start camera with provided ID
            logger.info("Opening camera with id: " + cameraID);
            videoCapture.open(cameraID);
            videoCapture.set(Videoio.CAP_PROP_FRAME_WIDTH, settingsContainer.frameWidth);
            videoCapture.set(Videoio.CAP_PROP_FRAME_HEIGHT, settingsContainer.frameHeight);
            if (settingsContainer.disableAutoExposure)
                videoCapture.set(Videoio.CAP_PROP_AUTO_EXPOSURE, 0);
            if (settingsContainer.disableAutoWB)
                videoCapture.set(Videoio.CAP_PROP_AUTO_WB, 0);
            if (settingsContainer.disableAutoFocus)
                videoCapture.set(Videoio.CAP_PROP_AUTOFOCUS, 0);
            videoCapture.set(Videoio.CAP_PROP_EXPOSURE, settingsContainer.maxExposure);

            // Capture the first frame
            videoCapture.read(frame);

            // Check if camera opened and first frame is not empty
            if (videoCapture.isOpened() && !frame.empty()) {
                openCVRunning = true;
                logger.info("Camera " + cameraID + " opened!");
                return true;
            }
        } catch (Exception e) {
            logger.error("Error opening camera " + cameraID + " !", e);
        }
        return false;
    }

    /**
     * Processes the frame, finds ARUco,
     * estimates its position and sends coordinates to PositionHandler class
     */
    @Override
    public void run() {
        // Create ARUco parameters (adaptive thresholding) from settings
        DetectorParameters detectorParameters = DetectorParameters.create();
        detectorParameters.set_adaptiveThreshConstant(settingsContainer.adaptiveThreshConstant);

        // Create local variable to calculate yaw angle
        double yaw = 0;

        // Transfer the current frame to the OSDHandler class
        osdHandler.setSourceFrame(frame);

        while (openCVRunning && videoCapture.isOpened()) {
            // Wait for the frame to be read
            if (videoCapture.read(frame) && !frame.empty()) {
                try {
                    // Convert current frame to grayscale
                    Imgproc.cvtColor(frame, gray, Imgproc.COLOR_RGB2GRAY);

                    // Check gray frame
                    positionContainer.isFrameNormal = !gray.empty();

                    // Detect ARUco markers
                    MatOfInt ids = new MatOfInt();
                    List<Mat> corners = new ArrayList<>();
                    List<Mat> rejectedImgPoints = new ArrayList<>();
                    Aruco.detectMarkers(gray, dictionary, corners, ids,
                            detectorParameters, rejectedImgPoints, cameraMatrix, cameraDistortions);

                    // Print warning message if more than one marker detected
                    if (ids.total() > 1)
                        logger.warn("More than one marker found!");

                    // Make sure that only one marker was found and it is allowed
                    if (ids.total() == 1 && settingsContainer.allowedIDs.contains((int)ids.get(0, 0)[0])) {

                        // Estimate position of the marker
                        Mat rVec = new Mat();
                        Mat tVec = new Mat();
                        Mat rMat = new Mat();
                        Aruco.estimatePoseSingleMarkers(corners, settingsContainer.markerSize,
                                cameraMatrix, cameraDistortions, rVec, tVec);
                        double[] tArr = tVec.get(0, 0);

                        // Calculate euler angles (only yaw) from rVec
                        Calib3d.Rodrigues(rVec, rMat);
                        if (Math.sqrt(rMat.get(0, 0)[0] * rMat.get(0, 0)[0] +
                                rMat.get(1, 0)[0] * rMat.get(1, 0)[0]) >= 1e-6) {
                            yaw = toDegrees(Math.atan2(rMat.get(1, 0)[0], rMat.get(0, 0)[0]) +
                                    Math.PI / 2);
                            if (yaw > 180)
                                yaw -= 360;
                        }

                        // Calculate marker's center if video stream is enabled
                        if (osdHandler.isStreamEnabled()) {
                            // (TopLeftX + BottomRightX) / 2, (TopLeftY + BottomRightY) / 2
                            Point center = new Point(((int) corners.get(0).get(0, 0)[0]
                                    + (int) corners.get(0).get(0, 2)[0]) / 2.0,
                                    ((int) corners.get(0).get(0, 0)[1]
                                            + (int) corners.get(0).get(0, 2)[1]) / 2.0);
                            center.x = positionContainer.frameCurrent.x * settingsContainer.inputFilter
                                    + center.x * (1 - settingsContainer.inputFilter);
                            center.y = positionContainer.frameCurrent.y * settingsContainer.inputFilter
                                    + center.y * (1 - settingsContainer.inputFilter);
                            positionContainer.frameCurrent = center;
                        }

                        // Transfer estimated position of the marker to the PositionHandler class
                        positionHandler.proceedPosition(true, tArr[0], tArr[1], tArr[2], yaw);
                    } else
                        // If no correct markers detected
                        positionHandler.proceedPosition(false);

                    // Adjust camera exposure
                    adaptiveExposure();

                    // Calculate FPS
                    framesCount++;
                    long timeCurrent = System.currentTimeMillis();
                    if (timeCurrent - timeStart > settingsContainer.fpsMeasurePeriod) {
                        // If 'fps_measure_period' passes
                        double fps = (double) framesCount / (timeCurrent - timeStart) * 1000.0;

                        // Transfer FPS to the OSD Class and log it
                        osdHandler.setFps(decimalFormat.format(fps));
                        if (settingsContainer.logFPS)
                            logger.info("FPS: " + decimalFormat.format(fps));
                        framesCount = 0;

                        // Restart timer
                        timeStart = System.currentTimeMillis();
                    }

                    // Push frame to the OSD class
                    osdFramesCounter++;
                    if (osdFramesCounter > pushOSDAfterFrames) {
                        osdFramesCounter = 0;
                        osdHandler.proceedNewFrame();
                    }
                } catch (Exception e) {
                    positionContainer.isFrameNormal = false;
                    logger.error("Error processing the frame!", e);
                    positionHandler.proceedPosition(false);
                }
            } else {
                positionContainer.isFrameNormal = false;
                logger.error("Can't read the frame!");
                positionHandler.proceedPosition(false);
            }
        }
    }

    /**
     * Dynamically adjusts camera exposure and managing backlight based on light levels
     */
    private void adaptiveExposure() {
        // Current illumination value
        double illumination = -1.;

        // Telemetry has a higher priority than the platform
        if (!telemetryContainer.telemetryLost && telemetryContainer.illumination > 0)
            illumination = telemetryContainer.illumination;
        else if (!platformContainer.platformLost && platformContainer.illumination > 0)
            illumination = platformContainer.illumination;

        // Calculate exposure correction
        double newExposure = Math.log((Math.pow(1. / settingsContainer.cameraAperture, 2) * 12.5)
                / (illumination * settingsContainer.cameraISO) * 1000) / 0.30102999566;

        // Crop new value
        if (newExposure > settingsContainer.maxExposure)
            newExposure = settingsContainer.maxExposure;

        // Set new exposure to camera
        if (abs(platformContainer.cameraExposure - newExposure) > 0.5 && settingsContainer.disableAutoExposure) {
            platformContainer.cameraExposure = newExposure;
            videoCapture.set(Videoio.CAP_PROP_EXPOSURE, platformContainer.cameraExposure);
        }

        // Turn on backlight if current mode is not IDLE or DONE
        if (positionContainer.status != PositionContainer.STATUS_IDLE
                && positionContainer.status != PositionContainer.STATUS_DONE) {
            // Turn on backlight in low light or in modes STAB, STAB, PREV and LOST (optical stabilization)
            if ((platformContainer.illumination < settingsContainer.platformLightEnableThreshold
                    || positionContainer.status == PositionContainer.STATUS_STAB
                    || positionContainer.status == PositionContainer.STATUS_LAND
                    || positionContainer.status == PositionContainer.STATUS_PREV
                    || positionContainer.status == PositionContainer.STATUS_LOST)
                    && !platformContainer.backlight)
                platformContainer.backlight = true;

            // Turn off backlight
            else if (platformContainer.illumination > settingsContainer.platformLightDisableThreshold
                    && platformContainer.backlight)
                platformContainer.backlight = false;
        }
        // Turn off backlight if current mode is IDLE or DONE
        else
            platformContainer.backlight = false;
    }

    /**
     * @return true if current openCV frame is empty
     */
    public boolean isFrameEmpty() {
        return frame.empty();
    }

    /**
     * Stops OpenCVHandler and releases the camera
     */
    public void stop() {
        logger.warn("Stopping OpenCV handler");
        openCVRunning = false;
        videoCapture.release();
    }
}
