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
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.text.DecimalFormat;
import java.util.Vector;

public class OSDHandler implements Runnable {
    private final Logger logger = Logger.getLogger(this.getClass().getSimpleName());
    private final DecimalFormat decimalFormatMono = new DecimalFormat("#");
    private final DecimalFormat decimalFormatSimple = new DecimalFormat("#.#");
    private final VideoStream videoStream;
    private boolean handlerRunning = true;
    private boolean streamEnabledLast = false;
    volatile public boolean streamEnabled = true, streamOnPageEnabled = true;
    volatile public boolean newPositionFlag = false;
    private final PositionContainer positionContainer;
    private final PlatformContainer platformContainer;
    
    public String fps = "NAN";
    public Mat sourceFrame, watermark;

    /**
     * Analogue of Arduino's map function with in_min = 1500
     */
    private int mapInt(int value, int in_max, int out_min, int out_max)
    {
        return (value - 1500) * (out_max - out_min) / (in_max - 1500) + out_min;
    }

    /**
     * This class takes a raw frame as input, draws the OSD and pushes it to the video stream
     * @param videoStream VideoStream class object
     */
    public OSDHandler(VideoStream videoStream,
                      PositionContainer positionContainer,
                      PlatformContainer platformContainer) {
        this.videoStream = videoStream;
        this.positionContainer = positionContainer;
        this.platformContainer = platformContainer;
    }

    /**
     * Draws OSD and pushes frame to the videoStream
     */
    public void proceedFrame() {
        try {
            if (streamEnabled && !streamEnabledLast) {
                // If new streamEnabled flag (true) is provided
                videoStream.start();
            } else if (!streamEnabled && streamEnabledLast) {
                // If new streamEnabled flag (false) is provided
                videoStream.stop();
            }
            streamEnabledLast = streamEnabled;
            if (newPositionFlag && streamEnabled && sourceFrame != null) {
                // Draw OSD if stream enabled and newPositionFlag provided
                Point setpoint = positionContainer.frameSetpoint;

                // Add watermark
                Mat destFrame = addWatermark(sourceFrame, watermark);

                // FPS white on black text
                Imgproc.putText(destFrame, fps + " FPS", new Point(40, 40),
                        Imgproc.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 0, 0), 2);
                Imgproc.putText(destFrame, fps + " FPS", new Point(40, 40),
                        Imgproc.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255, 255, 255), 1);

                // Main big circle
                Imgproc.circle(destFrame, setpoint, 200, new Scalar(200, 200, 200), 2);

                // Center of the marker
                Imgproc.circle(destFrame, positionContainer.frameCurrent,
                        20, new Scalar(200, 200, 200), 1);
                Imgproc.circle(destFrame, positionContainer.frameCurrent,
                        6, new Scalar(0, 0, 0), -1);
                Imgproc.circle(destFrame, positionContainer.frameCurrent,
                        5, new Scalar(255, 255, 255), -1);

                // Left white on black labels
                Imgproc.putText(destFrame, "X", new Point(setpoint.x - 220, setpoint.y - 8),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(0, 0, 0), 2);
                Imgproc.putText(destFrame, "Y", new Point(setpoint.x - 220, setpoint.y + 16),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(0, 0, 0), 2);
                Imgproc.putText(destFrame, "X", new Point(setpoint.x - 220, setpoint.y - 8),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(255, 255, 255), 1);
                Imgproc.putText(destFrame, "Y", new Point(setpoint.x - 220, setpoint.y + 16),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(255, 255, 255), 1);

                // Bottom white on black labels
                Imgproc.putText(destFrame, "YAW", new Point(setpoint.x - 12, setpoint.y + 220),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(0, 0, 0), 2);
                Imgproc.putText(destFrame, "YAW", new Point(setpoint.x - 12, setpoint.y + 220),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(255, 255, 255), 1);

                // Right white on black labels
                Imgproc.putText(destFrame, "A", new Point(setpoint.x + 212, setpoint.y - 12),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(0, 0, 0), 2);
                Imgproc.putText(destFrame, "L", new Point(setpoint.x + 212, setpoint.y + 4),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(0, 0, 0), 2);
                Imgproc.putText(destFrame, "T", new Point(setpoint.x + 212, setpoint.y + 20),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(0, 0, 0), 2);
                Imgproc.putText(destFrame, "A", new Point(setpoint.x + 212, setpoint.y - 12),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(255, 255, 255), 1);
                Imgproc.putText(destFrame, "L", new Point(setpoint.x + 212, setpoint.y + 4),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(255, 255, 255), 1);
                Imgproc.putText(destFrame, "T", new Point(setpoint.x + 212, setpoint.y + 20),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(255, 255, 255), 1);

                // Top white on black labels
                Imgproc.putText(destFrame, "STATUS", new Point(setpoint.x - 30, setpoint.y - 204),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(0, 0, 0), 2);
                Imgproc.putText(destFrame, "STATUS", new Point(setpoint.x - 30, setpoint.y - 204),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(255, 255, 255), 1);

                // Left green data (current absolute coordinates)
                Imgproc.putText(destFrame, decimalFormatMono.format(positionContainer.x) + " cm",
                        new Point(setpoint.x - 190, setpoint.y - 8),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(0, 255, 0), 2);
                Imgproc.putText(destFrame, decimalFormatMono.format(positionContainer.y) + " cm",
                        new Point(setpoint.x - 190, setpoint.y + 16),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(0, 255, 0), 2);

                // Bottom green data (current yaw angle)
                Imgproc.putText(destFrame, decimalFormatMono.format(positionContainer.yaw) + " deg",
                        new Point(setpoint.x - (decimalFormatMono.format(positionContainer.yaw) + " deg").length() * 5,
                                setpoint.y + 190),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(0, 255, 0), 2);

                // Right green data (current absolute altitude)
                Imgproc.putText(destFrame, decimalFormatMono.format(positionContainer.z) + " cm",
                        new Point(setpoint.x + 190 -
                                (decimalFormatMono.format(positionContainer.z) + " cm").length() * 10, setpoint.y + 2),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(0, 255, 0), 2);

                // Bottom left white on black labels
                Imgproc.putText(destFrame, "EXP", new Point(setpoint.x - 120, setpoint.y + 110),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(0, 0, 0), 2);
                Imgproc.putText(destFrame, "EXP", new Point(setpoint.x - 120, setpoint.y + 110),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(255, 255, 255), 1);

                // Bottom right white on black labels
                Imgproc.putText(destFrame, "SPD", new Point(setpoint.x + 90, setpoint.y + 110),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(0, 0, 0), 2);
                Imgproc.putText(destFrame, "SPD", new Point(setpoint.x + 90, setpoint.y + 110),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(255, 255, 255), 1);

                // Top green data (status)
                switch (positionContainer.status) {
                    case 1:
                        // STAB
                        Imgproc.putText(destFrame, "STAB",
                                new Point(setpoint.x - 20, setpoint.y - 174),
                                Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(0, 255, 0), 2);
                        break;
                    case 2:
                        // LAND
                        Imgproc.putText(destFrame, "LAND",
                                new Point(setpoint.x - 20, setpoint.y - 174),
                                Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(0, 255, 127), 2);
                        break;
                    case 3:
                        // PREV
                        Imgproc.putText(destFrame, "PREV",
                                new Point(setpoint.x - 20, setpoint.y - 174),
                                Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(0, 127, 255), 2);
                        break;
                    case 4:
                        // LOST
                        Imgproc.putText(destFrame, "LOST",
                                new Point(setpoint.x - 20, setpoint.y - 174),
                                Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(0, 0, 255), 2);
                        break;
                    case 5:
                        // DONE
                        Imgproc.putText(destFrame, "DONE",
                                new Point(setpoint.x - 20, setpoint.y - 174),
                                Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(255, 200, 0), 2);
                        break;
                    default:
                        // IDLE
                        Imgproc.putText(destFrame, "IDLE",
                                new Point(setpoint.x - 20, setpoint.y - 174),
                                Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(255, 200, 0), 2);
                        break;
                }

                // Bottom left green data (camera exposure)
                Imgproc.putText(destFrame, decimalFormatSimple.format(platformContainer.cameraExposure),
                        new Point(setpoint.x - 125,
                                setpoint.y + 130),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(0, 255, 0), 2);

                // Bottom right green data (abs speed)
                Imgproc.putText(destFrame, decimalFormatSimple.format(Math.abs(platformContainer.speed)) + " km/h",
                        new Point(setpoint.x + 70,
                                setpoint.y + 130),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(0, 255, 0), 2);

                // Yaw progress bars (Bottom)
                if (positionContainer.ddcYaw > 1520)
                    Imgproc.ellipse(destFrame, new Point(setpoint.x, setpoint.y), new Size(195, 195),
                            90, -14, mapInt(positionContainer.ddcYaw,
                                    2000, -15, -45), new Scalar(255, 200, 0), 5);
                else if (positionContainer.ddcYaw < 1480)
                    Imgproc.ellipse(destFrame, new Point(setpoint.x, setpoint.y), new Size(195, 195),
                            90, 14, mapInt(positionContainer.ddcYaw,
                                    1000, 15, 45), new Scalar(255, 200, 0), 5);

                // Z progress bars (Right)
                if (positionContainer.ddcZ > 1520)
                    Imgproc.ellipse(destFrame, new Point(setpoint.x, setpoint.y), new Size(195, 195),
                            0, -14, mapInt(positionContainer.ddcZ, 2000, -15, -45),
                            new Scalar(255, 200, 0), 5);
                else if (positionContainer.ddcZ < 1480)
                    Imgproc.ellipse(destFrame, new Point(setpoint.x, setpoint.y), new Size(195, 195),
                            0, 14, mapInt(positionContainer.ddcZ, 1000, 15, 45),
                            new Scalar(255, 200, 0), 5);

                // From top to bottom arrows (to the center). Bottom arc on the marker (Y)
                if (positionContainer.ddcY > 1520) {
                    // Clip ddcY to 1800
                    int stagedDirection = positionContainer.ddcY;
                    if (stagedDirection > 1800)
                        stagedDirection = 1800;
                    for (int i = 0; i < mapInt(stagedDirection, 1800, 1, 11); i++) {
                        Imgproc.line(destFrame, new Point(setpoint.x - 20 - i, setpoint.y - 45 - (16 * i)),
                                new Point(setpoint.x, setpoint.y - 25 - (16 * i)),
                                new Scalar(255, 200, 0), 2);
                        Imgproc.line(destFrame, new Point(setpoint.x, setpoint.y - 25 - (16 * i)),
                                new Point(setpoint.x + 20 + i, setpoint.y - 45 - (16 * i)),
                                new Scalar(255, 200, 0), 2);
                    }
                    Imgproc.ellipse(destFrame, positionContainer.frameCurrent, new Size(20, 20),
                            90, -45, 45, new Scalar(255, 200, 0), 2);
                }

                // From right to left arrows (to the center). Left arc on the marker (X)
                if (positionContainer.ddcX < 1480) {
                    // Clip ddcX to 1200
                    int stagedDirection = positionContainer.ddcX;
                    if (stagedDirection < 1200)
                        stagedDirection = 1200;
                    for (int i = 0; i < mapInt(stagedDirection, 1200, 1, 11); i++) {
                        Imgproc.line(destFrame, new Point(setpoint.x + 45 + (16 * i), setpoint.y - 20 - i),
                                new Point(setpoint.x + 25 + (16 * i), setpoint.y),
                                new Scalar(255, 200, 0), 2);
                        Imgproc.line(destFrame, new Point(setpoint.x + 25 + (16 * i), setpoint.y),
                                new Point(setpoint.x + 45 + (16 * i), setpoint.y + 20 + i),
                                new Scalar(255, 200, 0), 2);
                    }
                    Imgproc.ellipse(destFrame, positionContainer.frameCurrent, new Size(20, 20),
                            180, -45, 45, new Scalar(255, 200, 0), 2);
                }

                // From bottom to top arrows (to the center). Top arc on the marker (Y)
                if (positionContainer.ddcY < 1480) {
                    // Clip ddcY to 1200
                    int stagedDirection = positionContainer.ddcY;
                    if (stagedDirection < 1200)
                        stagedDirection = 1200;
                    for (int i = 0; i < mapInt(stagedDirection, 1200, 1, 11); i++) {
                        Imgproc.line(destFrame, new Point(setpoint.x - 20 - i, setpoint.y + 45 + (16 * i)),
                                new Point(setpoint.x, setpoint.y + 25 + (16 * i)),
                                new Scalar(255, 200, 0), 2);
                        Imgproc.line(destFrame, new Point(setpoint.x, setpoint.y + 25 + (16 * i)),
                                new Point(setpoint.x + 20 + i, setpoint.y + 45 + (16 * i)),
                                new Scalar(255, 200, 0), 2);
                    }
                    Imgproc.ellipse(destFrame, positionContainer.frameCurrent, new Size(20, 20),
                            -90, -45, 45, new Scalar(255, 200, 0), 2);
                }

                // Left, right (X)
                if (positionContainer.ddcX > 1520) {
                    // Clip ddcX to 1800
                    int stagedDirection = positionContainer.ddcX;
                    if (stagedDirection > 1800)
                        stagedDirection = 1800;
                    for (int i = 0; i < mapInt(stagedDirection, 1800, 1, 11); i++) {
                        Imgproc.line(destFrame, new Point(setpoint.x - 45 - (16 * i), setpoint.y - 20 - i),
                                new Point(setpoint.x - 25 - (16 * i), setpoint.y),
                                new Scalar(255, 200, 0), 2);
                        Imgproc.line(destFrame, new Point(setpoint.x - 25 - (16 * i), setpoint.y),
                                new Point(setpoint.x - 45 - (16 * i), setpoint.y + 20 + i),
                                new Scalar(255, 200, 0), 2);
                    }
                    Imgproc.ellipse(destFrame, positionContainer.frameCurrent, new Size(20, 20),
                            0, -45, 45, new Scalar(255, 200, 0), 2);
                }

                Imgproc.circle(destFrame, setpoint, 5, new Scalar(0, 255, 0), -1);
                Imgproc.rectangle(destFrame, new Point(setpoint.x - 20, setpoint.y - 20),
                        new Point(setpoint.x + 20, setpoint.y + 20), new Scalar(0, 255, 0), 1);

                // Transfer frame with OSD to the VideoStream class
                videoStream.frame = destFrame;
                // Push current frame to the web page
                videoStream.pushFrame();
                // Reset new position flag
                newPositionFlag = false;
            }
        } catch (Exception e) {
            logger.error("Error processing frame!", e);
        }
    }

    /**
     * Combines 2 Mats
     * @param image source image
     * @param watermark transparent RGBA image
     * @return image with watermark
     */
    private Mat addWatermark(Mat image, Mat watermark) {
        Mat destFrame = image.clone();
        Mat logo = new Mat();
        Mat logo_alpha;
        Vector<Mat> rgba = new Vector<>();
        Core.split(watermark, rgba);
        logo_alpha = rgba.get(3);
        rgba.remove(rgba.size() - 1);
        Core.merge(rgba, logo);
        Rect roi = new Rect(destFrame.cols() - logo.cols(), 0, logo.cols(), logo.rows());
        Mat imageROI = destFrame.submat(roi);
        logo.copyTo(imageROI, logo_alpha);
        return destFrame;
    }

    /**
     * Loads watermark and starts proceedFrame in a while
     */
    @Override
    public void run() {
        watermark = FileWorkers.loadImageAsMat(Main.settings.get("watermark_file").getAsString());
        while (handlerRunning)
            proceedFrame();
    }

    /**
     * Closes videoStream and sets handlerRunning flag
     */
    public void stop() {
        handlerRunning = false;
        videoStream.stop();
    }
}
