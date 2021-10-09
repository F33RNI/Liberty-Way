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
    private boolean streamEnabled = false;
    private boolean streamEnabledLast = false;
    private boolean newFrameFlag = false;
    private String fps = "0";
    private Mat sourceFrame, watermark;
    private volatile boolean handlerRunning = true;

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
            if (newFrameFlag && streamEnabled && sourceFrame != null && !sourceFrame.empty()) {
                // Reset flag
                newFrameFlag = false;

                // Draw OSD if stream enabled and newPositionFlag provided
                Point setpoint = positionContainer.frameSetpoint;

                // Add watermark
                Mat destFrame = addWatermark(sourceFrame, watermark);

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
                        new Point(setpoint.x -
                                (decimalFormatMono.format(positionContainer.yaw) + " deg").length() * 5,
                                setpoint.y + 190),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(0, 255, 0), 2);

                // Right green data (current absolute altitude)
                Imgproc.putText(destFrame, decimalFormatMono.format(positionContainer.z) + " cm",
                        new Point(setpoint.x + 190 -
                                (decimalFormatMono.format(positionContainer.z) + " cm").length() * 10,
                                setpoint.y + 2),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(0, 255, 0), 2);

                // Bottom left white on black labels
                Imgproc.putText(destFrame, "FPS", new Point(setpoint.x - 120, setpoint.y + 110),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(0, 0, 0), 2);
                Imgproc.putText(destFrame, "FPS", new Point(setpoint.x - 120, setpoint.y + 110),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(255, 255, 255), 1);

                // Bottom right white on black labels
                Imgproc.putText(destFrame, "EXP", new Point(setpoint.x + 90, setpoint.y + 110),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(0, 0, 0), 2);
                Imgproc.putText(destFrame, "EXP", new Point(setpoint.x + 90, setpoint.y + 110),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(255, 255, 255), 1);

                // Top green data (status)
                Scalar statusColor;

                switch (positionContainer.status) {
                    case 1:
                    case 2:
                        // MKWT or WAYP
                        statusColor = new Scalar(255, 0, 255);
                        break;
                    case 3:
                        // STAB
                        statusColor = new Scalar(0, 255, 0);
                        break;
                    case 4:
                        // LAND
                        statusColor = new Scalar(0, 255, 127);
                        break;
                    case 5:
                        // PREV
                        statusColor = new Scalar(0, 127, 255);
                        break;
                    case 6:
                        // LOST
                        statusColor = new Scalar(0, 0, 255);
                        break;
                    case 7:
                        // DONE
                        statusColor = new Scalar(255, 255, 0);
                        break;
                    default:
                        // WAIT or any other
                        statusColor = new Scalar(255, 200, 0);
                        break;
                }

                Imgproc.putText(destFrame, positionContainer.getStatusString(),
                        new Point(setpoint.x - 20, setpoint.y - 174),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, statusColor, 2);


                // Bottom left green data (fps)
                Imgproc.putText(destFrame, fps, new Point(setpoint.x - 120, setpoint.y + 130),
                        Imgproc.FONT_HERSHEY_PLAIN, 1, new Scalar(0, 255, 0), 2);

                // Bottom right green data (camera exposure)
                Imgproc.putText(destFrame, decimalFormatSimple.format(platformContainer.cameraExposure),
                        new Point(setpoint.x + 85, setpoint.y + 130),
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
                // Push current frame to the web page
                videoStream.pushFrame(destFrame);
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
     * @return are JPEG stream and OSD enabled
     */
    public boolean isStreamEnabled() {
        return this.streamEnabled;
    }

    /**
     * If the newFrameFlag is provided, a new OSD frame will be calculated.
     */
    public void setNewFrameFlag(boolean newFrameFlag) {
        this.newFrameFlag = newFrameFlag;
    }

    /**
     * Enables JPEG video stream with OSD
     */
    public void enableStreamAndOSD() {
        this.streamEnabled = true;
    }

    /**
     * Disables JPEG video stream with OSD
     */
    public void disableStreamAndOSD() {
        this.streamEnabled = false;
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
        watermark = FileWorkers.loadImageAsMat(new File("watermark.png"));
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
