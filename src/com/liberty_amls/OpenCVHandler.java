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

import static java.lang.Math.toDegrees;

public class OpenCVHandler implements Runnable {
    private final Logger logger = Logger.getLogger(this.getClass().getSimpleName());
    private final SettingsContainer settingsContainer;
    private final PositionHandler positionHandler;
    private final PositionContainer positionContainer;
    private final PlatformHandler platformHandler;
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
                         PlatformHandler platformHandler,
                         OSDHandler osdHandler,
                         SettingsContainer settingsContainer) {
        this.cameraID = cameraID;
        this.videoCapture = videoCapture;
        this.positionHandler = positionHandler;
        this.positionContainer = positionContainer;
        this.platformHandler = platformHandler;
        this.osdHandler = osdHandler;
        this.settingsContainer = settingsContainer;
        framesCount = 0;
    }

    /**
     * Loads settings and opens the camera
     */
    public void start() {
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
            videoCapture.open(cameraID);
            videoCapture.set(Videoio.CAP_PROP_FRAME_WIDTH, settingsContainer.frameWidth);
            videoCapture.set(Videoio.CAP_PROP_FRAME_HEIGHT, settingsContainer.frameHeight);
            if (settingsContainer.disableAutoExposure)
                videoCapture.set(Videoio.CAP_PROP_AUTO_EXPOSURE, 0);
            if (settingsContainer.disableAutoWB)
                videoCapture.set(Videoio.CAP_PROP_AUTO_WB, 0);
            if (settingsContainer.disableAutoFocus)
                videoCapture.set(Videoio.CAP_PROP_AUTOFOCUS, 0);
            videoCapture.set(Videoio.CAP_PROP_EXPOSURE, settingsContainer.defaultExposure);
            openCVRunning = true;
            if (!videoCapture.isOpened()) {
                return;
            }

            // Capture the first frame
            videoCapture.read(frame);

            // Set flag in platformHandler class
            platformHandler.setOpencvStarts(true);

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
        detectorParameters.set_adaptiveThreshConstant(settingsContainer.adaptiveThreshConstant);

        // Transfer the current frame to the OSDHandler class
        osdHandler.setSourceFrame(frame);

        while (openCVRunning && videoCapture.isOpened()) {
            // Wait for the frame to be read
            if (videoCapture.read(frame)) {
                try {
                    // Push frame to the OSD class
                    osdFramesCounter++;
                    if (osdFramesCounter > pushOSDAfterFrames) {
                        osdFramesCounter = 0;
                        osdHandler.setNewFrameFlag(true);
                    }

                    // Convert current frame to grayscale
                    Imgproc.cvtColor(frame, gray, Imgproc.COLOR_RGB2GRAY);

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
                        double yaw = 0;
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

                    // Calculate FPS
                    framesCount++;
                    long timeCurrent = System.currentTimeMillis();
                    if (timeCurrent - timeStart > settingsContainer.fpsMeasurePeriod) {
                        // If 'fps_measure_period' passes
                        double fps = (double) framesCount / (timeCurrent - timeStart) * 1000.0;

                        // Transfer FPS to the OSD Class and log it
                        osdHandler.setFps(decimalFormat.format(fps));
                        logger.info("FPS: " + decimalFormat.format(fps));
                        framesCount = 0;

                        // Restart timer
                        timeStart = System.currentTimeMillis();
                    }
                } catch (Exception e) {
                    logger.error("Error processing the frame!", e);
                    positionHandler.proceedPosition(false);
                }
            } else {
                logger.error("Can't read the frame!");
                positionHandler.proceedPosition(false);
            }
        }
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
