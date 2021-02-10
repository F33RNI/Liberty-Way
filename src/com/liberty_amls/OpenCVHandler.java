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

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import org.apache.log4j.Logger;
import org.opencv.aruco.Aruco;
import org.opencv.aruco.DetectorParameters;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.toDegrees;

public class OpenCVHandler implements Runnable {
    private final Logger logger = Logger.getLogger(this.getClass().getSimpleName());
    private final PositionHandler positionHandler;
    private final int cameraID, exposure;
    private final float markerSize;
    private JsonArray allowedIDs;
    private boolean openCVRunning;
    private VideoCapture videoCapture;
    private int framesCount;
    private int fpsMeasurePeriod;
    private long timeStart;
    private final DecimalFormat decimalFormat = new DecimalFormat("#.#");
    private Mat cameraMatrix, cameraDistortions;
    public Mat frame;

    /**
     * This class reads frame from camera, estimate ARUco marker position
     * and provides it to the other classes
     * @param cameraID Camera identifier
     * @param exposure Camera exposure
     * @param markerSize ARUco marker size (length)
     * @param positionHandler positionHandler class object (position processing)
     */
    public OpenCVHandler(int cameraID, int exposure, float markerSize, PositionHandler positionHandler) {
        this.cameraID = cameraID;
        this.exposure = exposure;
        this.markerSize = markerSize;
        this.positionHandler = positionHandler;
        framesCount = 0;
    }

    /**
     * Loads settings and opens the camera
     */
    public void start() {
        try {
            // Load camera corrections from jsons
            cameraMatrix = FileWorkers.loadCameraMatrix();
            cameraDistortions = FileWorkers.loadCameraDistortions();

            // Load settings
            int frameWidth = Main.settings.get("frame_width").getAsInt();
            int frameHeight = Main.settings.get("frame_height").getAsInt();
            fpsMeasurePeriod = Main.settings.get("fps_measure_period").getAsInt();
            allowedIDs = Main.settings.get("allowed_ids").getAsJsonArray();
            positionHandler.osdHandler.setpoint = new Point(frameWidth / 2.0, frameHeight / 2.0);
            videoCapture = new VideoCapture();

            // Start camera with provided ID
            videoCapture.open(cameraID);
            videoCapture.set(Videoio.CAP_PROP_FRAME_WIDTH, frameWidth);
            videoCapture.set(Videoio.CAP_PROP_FRAME_HEIGHT, frameHeight);
            if (Main.settings.get("disable_auto_exposure").getAsBoolean())
                videoCapture.set(Videoio.CAP_PROP_AUTO_EXPOSURE, 0);
            if (Main.settings.get("disable_auto_wb").getAsBoolean())
                videoCapture.set(Videoio.CAP_PROP_AUTO_WB, 0);
            if (Main.settings.get("disable_auto_focus").getAsBoolean())
                videoCapture.set(Videoio.CAP_PROP_AUTOFOCUS, 0);
            videoCapture.set(Videoio.CAP_PROP_EXPOSURE, exposure);
            openCVRunning = true;
            if (!videoCapture.isOpened()) {
                return;
            }
            frame = new Mat();

            // Capture the first frame
            videoCapture.read(frame);

            // Enable or disable stream by settings
            positionHandler.osdHandler.streamEnabled = Main.settings.get("video_enabled_by_default").getAsBoolean();
            logger.info("Camera " + cameraID + " opened!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Processes the frame, finds ARUco,
     * estimates its position and sends coordinates to PositionHandler class
     */
    @Override
    public void run() {
        // Create ARUco parameters (adaptive thresholding) from settings
        DetectorParameters detectorParameters = DetectorParameters.create();
        detectorParameters.set_adaptiveThreshConstant(Main.settings.get("adaptive_thresh_constant").getAsInt());

        Mat gray = new Mat();
        while (openCVRunning && videoCapture.isOpened()) {
            // Wait for the frame to be read
            if (videoCapture.read(frame)) {
                try {
                    // Transfer the current frame to the OSDHandler class
                    positionHandler.osdHandler.sourceFrame = frame;

                    // Convert current frame to grayscale
                    Imgproc.cvtColor(frame, gray, Imgproc.COLOR_RGB2GRAY);

                    // Detect ARUco markers
                    MatOfInt ids = new MatOfInt();
                    List<Mat> corners = new ArrayList<>();
                    List<Mat> rejectedImgPoints = new ArrayList<>();
                    Aruco.detectMarkers(gray, Aruco.getPredefinedDictionary(Aruco.DICT_4X4_250), corners, ids,
                            detectorParameters, rejectedImgPoints, cameraMatrix, cameraDistortions);

                    // Print warning message if more than one marker detected
                    if (ids.total() > 1)
                        logger.warn("More than one marker found!");

                    // Make sure that only one marker was found and it is allowed
                    if (ids.total() == 1 && allowedIDs.contains(new JsonPrimitive((int)ids.get(0, 0)[0]))) {

                        // Estimate position of the marker
                        Mat rVec = new Mat();
                        Mat tVec = new Mat();
                        Mat rMat = new Mat();
                        Aruco.estimatePoseSingleMarkers(corners, markerSize,
                                cameraMatrix, cameraDistortions, rVec, tVec);
                        double[] tArr = tVec.get(0, 0);

                        // Calculate euler angles (only yaw) from rVec
                        Calib3d.Rodrigues(rVec, rMat);
                        double yaw = 0;
                        if (Math.sqrt(rMat.get(0, 0)[0] * rMat.get(0, 0)[0] +
                                rMat.get(1, 0)[0] * rMat.get(1, 0)[0]) >= 1e-6) {
                            yaw = toDegrees(Math.atan2(rMat.get(1, 0)[0], rMat.get(0, 0)[0]) +
                                    Math.PI / 2);
                            if (yaw > 180)
                                yaw -= 360;
                        }

                        // Calculate marker's center if video stream is enabled
                        if (positionHandler.osdHandler.streamEnabled) {
                            // (TopLeftX + BottomRightX) / 2, (TopLeftY + BottomRightY) / 2
                            Point center = new Point(((int) corners.get(0).get(0, 0)[0]
                                    + (int) corners.get(0).get(0, 2)[0]) / 2.0,
                                    ((int) corners.get(0).get(0, 0)[1]
                                            + (int) corners.get(0).get(0, 2)[1]) / 2.0);
                            center.x = positionHandler.osdHandler.current.x * positionHandler.filterKoeff
                                    + center.x * (1 - positionHandler.filterKoeff);
                            center.y = positionHandler.osdHandler.current.y * positionHandler.filterKoeff
                                    + center.y * (1 - positionHandler.filterKoeff);
                            positionHandler.osdHandler.current = center;
                        }

                        // Transfer estimated position of the marker to the PositionHandler class
                        positionHandler.newPosition(tArr[0], tArr[1], tArr[2], yaw);
                    } else
                        // If no correct markers detected
                        positionHandler.noMarker();

                    // Calculate FPS
                    framesCount++;
                    long timeCurrent = System.currentTimeMillis();
                    if (timeCurrent - timeStart > fpsMeasurePeriod) {
                        // If 'fps_measure_period' passes
                        double fps = (double) framesCount / (timeCurrent - timeStart) * 1000.0;

                        // Transfer FPS to the OSD Class and log it
                        positionHandler.osdHandler.fps = decimalFormat.format(fps);
                        logger.info("FPS: " + decimalFormat.format(fps));
                        framesCount = 0;

                        // Restart timer
                        timeStart = System.currentTimeMillis();
                    }
                } catch (Exception e) {
                    logger.error("Error processing the frame!", e);
                    positionHandler.noMarker();
                }
            } else {
                logger.error("Can't read the frame!");
                positionHandler.noMarker();
            }
        }
    }

    /**
     * Stops OpenCVHandler and releases the camera
     */
    public void stop() {
        openCVRunning = false;
        videoCapture.release();
    }
}
