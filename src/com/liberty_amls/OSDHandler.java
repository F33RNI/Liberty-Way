/*
 * Copyright (C) 2022 Fern Lane, Liberty-Way UAS controller
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
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Vector;

public class OSDHandler implements Runnable {

    private final Logger logger = Logger.getLogger(this.getClass().getSimpleName());

    private final DecimalFormat decimalFormatMono = new DecimalFormat("#");
    private final DecimalFormat decimalFormatSimple = new DecimalFormat("#.#");

    private final VideoStream videoStream;
    private final PositionContainer positionContainer;
    private final PlatformContainer platformContainer;
    private final DroneCameraHandler droneCameraHandler;

    private boolean streamEnabled = false;
    private boolean streamEnabledLast = false;
    private String fps = "0";
    private Mat sourceFrame, watermark, watermarkResized, matWithWatermark, matWithOSD, matSmall;
    private volatile boolean handlerRunning;

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
                      PlatformContainer platformContainer,
                      DroneCameraHandler droneCameraHandler) {
        this.videoStream = videoStream;
        this.positionContainer = positionContainer;
        this.platformContainer = platformContainer;
        this.droneCameraHandler = droneCameraHandler;
    }

    /**
     * Draws OSD and pushes frame to the videoStream
     */
    private void proceedFrame() {
        try {
            // Copy frame setpoint
            Point setpoint = positionContainer.frameSetpoint;

            // Copy platform camera frame
            sourceFrame.copyTo(matWithOSD);

            // Draw OSD only in optical stabilization mode
            if (droneCameraHandler.getFrame() == null
                    || droneCameraHandler.getFrame().empty()
                    || droneCameraHandler.getFrame().width() < 10
                    || positionContainer.status == PositionContainer.STATUS_STAB
                    || positionContainer.status == PositionContainer.STATUS_LAND
                    || positionContainer.status == PositionContainer.STATUS_PREV
                    || positionContainer.status == PositionContainer.STATUS_LOST) {

                // Main big circle
                Imgproc.circle(matWithOSD, setpoint, 200, new Scalar(200, 200, 200), 2);

                // Center of the marker
                Imgproc.circle(matWithOSD, positionContainer.frameCurrent,
                        20, new Scalar(200, 200, 200), 1);
                Imgproc.circle(matWithOSD, positionContainer.frameCurrent,
                        6, new Scalar(0, 0, 0), -1);
                Imgproc.circle(matWithOSD, positionContainer.frameCurrent,
                        5, new Scalar(255, 255, 255), -1);

                // Left white on black labels
                Imgproc.putText(matWithOSD, "X", new Point(setpoint.x - 220, setpoint.y - 8),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(0, 0, 0), 2);
                Imgproc.putText(matWithOSD, "Y", new Point(setpoint.x - 220, setpoint.y + 16),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(0, 0, 0), 2);
                Imgproc.putText(matWithOSD, "X", new Point(setpoint.x - 220, setpoint.y - 8),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(255, 255, 255), 1);
                Imgproc.putText(matWithOSD, "Y", new Point(setpoint.x - 220, setpoint.y + 16),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(255, 255, 255), 1);

                // Bottom white on black labels
                Imgproc.putText(matWithOSD, "YAW", new Point(setpoint.x - 12, setpoint.y + 220),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(0, 0, 0), 2);
                Imgproc.putText(matWithOSD, "YAW", new Point(setpoint.x - 12, setpoint.y + 220),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(255, 255, 255), 1);

                // Right white on black labels
                Imgproc.putText(matWithOSD, "A", new Point(setpoint.x + 212, setpoint.y - 12),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(0, 0, 0), 2);
                Imgproc.putText(matWithOSD, "L", new Point(setpoint.x + 212, setpoint.y + 4),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(0, 0, 0), 2);
                Imgproc.putText(matWithOSD, "T", new Point(setpoint.x + 212, setpoint.y + 20),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(0, 0, 0), 2);
                Imgproc.putText(matWithOSD, "A", new Point(setpoint.x + 212, setpoint.y - 12),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(255, 255, 255), 1);
                Imgproc.putText(matWithOSD, "L", new Point(setpoint.x + 212, setpoint.y + 4),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(255, 255, 255), 1);
                Imgproc.putText(matWithOSD, "T", new Point(setpoint.x + 212, setpoint.y + 20),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(255, 255, 255), 1);

                // Top white on black labels
                Imgproc.putText(matWithOSD, "STATUS", new Point(setpoint.x - 30, setpoint.y - 204),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(0, 0, 0), 2);
                Imgproc.putText(matWithOSD, "STATUS", new Point(setpoint.x - 30, setpoint.y - 204),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(255, 255, 255), 1);

                // Left green data (current absolute coordinates)
                Imgproc.putText(matWithOSD, decimalFormatMono.format(positionContainer.x) + " cm",
                        new Point(setpoint.x - 190, setpoint.y - 8),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(0, 255, 0), 2);
                Imgproc.putText(matWithOSD, decimalFormatMono.format(positionContainer.y) + " cm",
                        new Point(setpoint.x - 190, setpoint.y + 16),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(0, 255, 0), 2);

                // Bottom green data (current yaw angle)
                Imgproc.putText(matWithOSD, decimalFormatMono.format(positionContainer.yaw) + " deg",
                        new Point(setpoint.x -
                                (decimalFormatMono.format(positionContainer.yaw) + " deg").length() * 5,
                                setpoint.y + 190),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(0, 255, 0), 2);

                // Right green data (current absolute altitude)
                Imgproc.putText(matWithOSD, decimalFormatMono.format(positionContainer.z) + " cm",
                        new Point(setpoint.x + 190 -
                                (decimalFormatMono.format(positionContainer.z) + " cm").length() * 10,
                                setpoint.y + 2),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(0, 255, 0), 2);

                // Bottom left white on black labels
                Imgproc.putText(matWithOSD, "FPS", new Point(setpoint.x - 120, setpoint.y + 110),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(0, 0, 0), 2);
                Imgproc.putText(matWithOSD, "FPS", new Point(setpoint.x - 120, setpoint.y + 110),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(255, 255, 255), 1);

                // Bottom right white on black labels
                Imgproc.putText(matWithOSD, "EXP", new Point(setpoint.x + 90, setpoint.y + 110),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(0, 0, 0), 2);
                Imgproc.putText(matWithOSD, "EXP", new Point(setpoint.x + 90, setpoint.y + 110),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(255, 255, 255), 1);

                // Top green data (status)
                Scalar statusColor;

                switch (positionContainer.status) {
                    case PositionContainer.STATUS_WAYP:
                        statusColor = new Scalar(255, 0, 255);
                        break;
                    case PositionContainer.STATUS_STAB:
                        statusColor = new Scalar(0, 255, 0);
                        break;
                    case PositionContainer.STATUS_LAND:
                        statusColor = new Scalar(0, 255, 127);
                        break;
                    case PositionContainer.STATUS_PREV:
                        statusColor = new Scalar(0, 127, 255);
                        break;
                    case PositionContainer.STATUS_LOST:
                        statusColor = new Scalar(0, 0, 255);
                        break;
                    case PositionContainer.STATUS_DONE:
                        statusColor = new Scalar(255, 255, 0);
                        break;
                    default:
                        statusColor = new Scalar(255, 200, 0);
                        break;
                }

                Imgproc.putText(matWithOSD, positionContainer.getStatusString(),
                        new Point(setpoint.x - 20, setpoint.y - 174),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, statusColor, 2);


                // Bottom left green data (fps)
                Imgproc.putText(matWithOSD, fps, new Point(setpoint.x - 120, setpoint.y + 130),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(0, 255, 0), 2);

                // Bottom right green data (camera exposure)
                Imgproc.putText(matWithOSD, decimalFormatSimple.format(platformContainer.cameraExposure),
                        new Point(setpoint.x + 85, setpoint.y + 130),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(0, 255, 0), 2);

                // Yaw progress bars (Bottom)
                if (positionContainer.ddcYaw > 1520)
                    Imgproc.ellipse(matWithOSD, new Point(setpoint.x, setpoint.y), new Size(195, 195),
                            90, -14, mapInt(positionContainer.ddcYaw,
                                    2000, -15, -45), new Scalar(255, 200, 0), 5);
                else if (positionContainer.ddcYaw < 1480)
                    Imgproc.ellipse(matWithOSD, new Point(setpoint.x, setpoint.y), new Size(195, 195),
                            90, 14, mapInt(positionContainer.ddcYaw,
                                    1000, 15, 45), new Scalar(255, 200, 0), 5);

                // Z progress bars (Right)
                if (positionContainer.ddcZ > 1520)
                    Imgproc.ellipse(matWithOSD, new Point(setpoint.x, setpoint.y), new Size(195, 195),
                            0, -14, mapInt(positionContainer.ddcZ, 2000, -15, -45),
                            new Scalar(255, 200, 0), 5);
                else if (positionContainer.ddcZ < 1480)
                    Imgproc.ellipse(matWithOSD, new Point(setpoint.x, setpoint.y), new Size(195, 195),
                            0, 14, mapInt(positionContainer.ddcZ, 1000, 15, 45),
                            new Scalar(255, 200, 0), 5);

                // From top to bottom arrows (to the center). Bottom arc on the marker (Y)
                if (positionContainer.ddcY > 1520) {
                    // Clip ddcY to 1800
                    int stagedDirection = positionContainer.ddcY;
                    if (stagedDirection > 1800)
                        stagedDirection = 1800;
                    for (int i = 0; i < mapInt(stagedDirection, 1800, 1, 11); i++) {
                        Imgproc.line(matWithOSD, new Point(setpoint.x - 20 - i, setpoint.y - 45 - (16 * i)),
                                new Point(setpoint.x, setpoint.y - 25 - (16 * i)),
                                new Scalar(255, 200, 0), 2);
                        Imgproc.line(matWithOSD, new Point(setpoint.x, setpoint.y - 25 - (16 * i)),
                                new Point(setpoint.x + 20 + i, setpoint.y - 45 - (16 * i)),
                                new Scalar(255, 200, 0), 2);
                    }
                    Imgproc.ellipse(matWithOSD, positionContainer.frameCurrent, new Size(20, 20),
                            90, -45, 45, new Scalar(255, 200, 0), 2);
                }

                // From right to left arrows (to the center). Left arc on the marker (X)
                if (positionContainer.ddcX < 1480) {
                    // Clip ddcX to 1200
                    int stagedDirection = positionContainer.ddcX;
                    if (stagedDirection < 1200)
                        stagedDirection = 1200;
                    for (int i = 0; i < mapInt(stagedDirection, 1200, 1, 11); i++) {
                        Imgproc.line(matWithOSD, new Point(setpoint.x + 45 + (16 * i), setpoint.y - 20 - i),
                                new Point(setpoint.x + 25 + (16 * i), setpoint.y),
                                new Scalar(255, 200, 0), 2);
                        Imgproc.line(matWithOSD, new Point(setpoint.x + 25 + (16 * i), setpoint.y),
                                new Point(setpoint.x + 45 + (16 * i), setpoint.y + 20 + i),
                                new Scalar(255, 200, 0), 2);
                    }
                    Imgproc.ellipse(matWithOSD, positionContainer.frameCurrent, new Size(20, 20),
                            180, -45, 45, new Scalar(255, 200, 0), 2);
                }

                // From bottom to top arrows (to the center). Top arc on the marker (Y)
                if (positionContainer.ddcY < 1480) {
                    // Clip ddcY to 1200
                    int stagedDirection = positionContainer.ddcY;
                    if (stagedDirection < 1200)
                        stagedDirection = 1200;
                    for (int i = 0; i < mapInt(stagedDirection, 1200, 1, 11); i++) {
                        Imgproc.line(matWithOSD, new Point(setpoint.x - 20 - i, setpoint.y + 45 + (16 * i)),
                                new Point(setpoint.x, setpoint.y + 25 + (16 * i)),
                                new Scalar(255, 200, 0), 2);
                        Imgproc.line(matWithOSD, new Point(setpoint.x, setpoint.y + 25 + (16 * i)),
                                new Point(setpoint.x + 20 + i, setpoint.y + 45 + (16 * i)),
                                new Scalar(255, 200, 0), 2);
                    }
                    Imgproc.ellipse(matWithOSD, positionContainer.frameCurrent, new Size(20, 20),
                            -90, -45, 45, new Scalar(255, 200, 0), 2);
                }

                // Left, right (X)
                if (positionContainer.ddcX > 1520) {
                    // Clip ddcX to 1800
                    int stagedDirection = positionContainer.ddcX;
                    if (stagedDirection > 1800)
                        stagedDirection = 1800;
                    for (int i = 0; i < mapInt(stagedDirection, 1800, 1, 11); i++) {
                        Imgproc.line(matWithOSD, new Point(setpoint.x - 45 - (16 * i), setpoint.y - 20 - i),
                                new Point(setpoint.x - 25 - (16 * i), setpoint.y),
                                new Scalar(255, 200, 0), 2);
                        Imgproc.line(matWithOSD, new Point(setpoint.x - 25 - (16 * i), setpoint.y),
                                new Point(setpoint.x - 45 - (16 * i), setpoint.y + 20 + i),
                                new Scalar(255, 200, 0), 2);
                    }
                    Imgproc.ellipse(matWithOSD, positionContainer.frameCurrent, new Size(20, 20),
                            0, -45, 45, new Scalar(255, 200, 0), 2);
                }

                Imgproc.circle(matWithOSD, setpoint, 5, new Scalar(0, 255, 0), -1);
                Imgproc.rectangle(matWithOSD, new Point(setpoint.x - 20, setpoint.y - 20),
                        new Point(setpoint.x + 20, setpoint.y + 20), new Scalar(0, 255, 0), 1);
            }

            // Result mat
            Mat destFrame;

            // Show only platform image if drone camera is not available
            if (droneCameraHandler.getFrame() == null || droneCameraHandler.getFrame().empty()
                    || droneCameraHandler.getFrame().width() < 10) {
                // Resize watermark
                double watermarkResizeK = matWithOSD.width() / 1280.0;
                Imgproc.resize(watermark, watermarkResized,
                        new Size(0, 0), watermarkResizeK, watermarkResizeK);

                // Add watermark
                destFrame = addWatermark(matWithOSD, watermarkResized);
            }

            // Both cameras are available
            else {
                // Show platform camera as main camera if current mode is optical stabilization
                if (positionContainer.status == PositionContainer.STATUS_STAB
                        || positionContainer.status == PositionContainer.STATUS_LAND
                        || positionContainer.status == PositionContainer.STATUS_PREV
                        || positionContainer.status == PositionContainer.STATUS_LOST) {

                    // Main camera is platform camera
                    destFrame = matWithOSD;

                    // Small camera is drone camera
                    double matSmallResizeK = (double)destFrame.height()
                            / (double)droneCameraHandler.getFrame().height() / 3.;
                    Imgproc.resize(droneCameraHandler.getFrame(), matSmall,
                            new Size(0, 0), matSmallResizeK, matSmallResizeK);

                }
                else {
                    // Main camera is drone camera
                    destFrame = droneCameraHandler.getFrame();

                    // Small camera is platform camera
                    double matSmallResizeK = (double)destFrame.height()
                            / (double)matWithOSD.height() / 3.;
                    Imgproc.resize(matWithOSD, matSmall,
                            new Size(0, 0), matSmallResizeK, matSmallResizeK);
                }

                // Add small image
                //matSmall.copyTo(destFrame.rowRange(destFrame.rows() - matSmall.rows(),
                //        destFrame.rows()).colRange(destFrame.cols() - matSmall.cols(), destFrame.cols()));
                matSmall.copyTo(destFrame.rowRange(0, matSmall.rows()).colRange(0, matSmall.cols()));

                // Add watermark
                double watermarkResizeK = destFrame.width() / 1280.0;
                Imgproc.resize(watermark, watermarkResized,
                        new Size(0, 0), watermarkResizeK, watermarkResizeK);
                destFrame = addWatermark(destFrame, watermarkResized);
            }

            // Transfer frame with OSD to the VideoStream class
            // Push current frame to the web page
            videoStream.pushFrame(destFrame);

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
        image.copyTo(matWithWatermark);
        Mat logo = new Mat();
        Mat logo_alpha;
        Vector<Mat> rgba = new Vector<>();
        Core.split(watermark, rgba);
        logo_alpha = rgba.get(3);
        rgba.remove(rgba.size() - 1);
        Core.merge(rgba, logo);
        Rect roi = new Rect(matWithWatermark.cols() - logo.cols(), 0, logo.cols(), logo.rows());
        Mat imageROI = matWithWatermark.submat(roi);
        logo.copyTo(imageROI, logo_alpha);
        return matWithWatermark;
    }

    private Mat resizeToFit(Mat matToResize, Mat destinationSizeMat) {
        int resizeKWidth = destinationSizeMat.width() / matToResize.width();
        int resizeKHeight = destinationSizeMat.height() / matToResize.height();
        int resizeK = Math.min(resizeKWidth, resizeKHeight);

        Mat resizedMat = new Mat();

        Imgproc.resize(matToResize, resizedMat,
                new Size(matToResize.width() * resizeK, matToResize.height() * resizeK));

        Mat backgroundMat = new Mat(destinationSizeMat.rows(), destinationSizeMat.cols(),
                destinationSizeMat.type(), Scalar.all(0));

        resizedMat.copyTo(backgroundMat.rowRange(1, 6).colRange(3, 10));

        return backgroundMat;

    }

    /**
     * @return are JPEG stream and OSD enabled
     */
    public boolean isStreamEnabled() {
        return this.streamEnabled;
    }

    /**
     * Draws a new OSD frame and pushes frame to the VideoStream
     */
    public void proceedNewFrame() {
        // Start or stop the stream
        if (streamEnabled && !streamEnabledLast) {
            // If new streamEnabled flag (true) is provided
            videoStream.start();
        } else if (!streamEnabled && streamEnabledLast) {
            // If new streamEnabled flag (false) is provided
            videoStream.stop();
        }
        streamEnabledLast = streamEnabled;

        // Continue only if the stream is on and the frame from the camera is not empty
        if (streamEnabled && sourceFrame != null && !sourceFrame.empty())
            synchronized (this) {
                notify();
            }
    }

    /**
     * Enables JPEG video stream with OSD
     */
    public void enableStreamAndOSD() {
        this.streamEnabled = true;
        System.gc();
    }

    /**
     * Disables JPEG video stream with OSD
     */
    public void disableStreamAndOSD() {
        this.streamEnabled = false;
        System.gc();
    }

    /**
     * Sets current FPS
     */
    public void setFps(String fps) {
        this.fps = fps;
    }

    /**
     * Sets current raw opencv frame (without OSD)
     */
    public void setSourceFrame(Mat sourceFrame) {
        this.sourceFrame = sourceFrame;
    }

    /**
     * Loads watermark and starts proceedFrame in a while
     */
    @Override
    public void run() {
        // Read watermark image
        watermark = FileWorkers.loadImageAsMat(new File("watermark.png"));

        // Initialize variables
        watermarkResized = new Mat();
        matWithWatermark = new Mat();
        matWithOSD = new Mat();
        matSmall = new Mat();

        // Start main loop
        handlerRunning = true;
        while (handlerRunning) {
            // Continue only if the stream is on and the frame from the camera is not empty
            if (streamEnabled && sourceFrame != null && !sourceFrame.empty())
                proceedFrame();

            // Pause current thread
            synchronized(this) {
                try {
                    wait();
                } catch (Exception ignored) { }
            }
        }
    }

    /**
     * Closes videoStream and sets handlerRunning flag
     */
    public void stop() {
        handlerRunning = false;
        videoStream.stop();
    }
}
