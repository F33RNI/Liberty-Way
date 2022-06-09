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
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

/**
 * This class provides the ability to receive an image from a second (FPV) camera
 */
public class DroneCameraHandler implements Runnable {

    private final Logger logger = Logger.getLogger(this.getClass().getSimpleName());

    private final String cameraIDString;
    private final Mat frame = new Mat();

    private VideoCapture videoCapture;
    private boolean handlerRunning = false;

    /**
     * @param cameraIDString ID of the camera (from OpenCV) as string
     */
    DroneCameraHandler(String cameraIDString) {
        this.cameraIDString = cameraIDString;
    }

    /**
     * Opens camera and starts loop
     */
    @Override
    public void run() {
        int cameraID = -1;

        // Parse camera ID String
        try {
            cameraID = Integer.parseInt(cameraIDString);
        } catch (Exception e) {
            logger.error("Error parsing camera ID. Drone camera will not be used", e);
        }

        // Continue only if the correct camera ID has been set
        if (cameraID >= 0) {
            try {
                // Create new VideoCapture class object
                videoCapture = new VideoCapture();

                // Open camera
                videoCapture.open(cameraID);

                // Capture the first frame
                videoCapture.read(frame);

                // Check if camera opened and first frame is not empty
                if (videoCapture.isOpened() && !frame.empty()) {
                    handlerRunning = true;
                    logger.info("Camera " + cameraID + " opened!");
                }
            } catch (Exception e) {
                logger.error("Error opening camera " + cameraID + " !", e);
            }

            // Main loop
            while (handlerRunning) {
                // Try to read the frame
                try {
                    videoCapture.read(frame);
                } catch (Exception ignored) { }
            }
        }
    }

    /**
     * @return frame from the second camera
     */
    public Mat getFrame() {
        return frame;
    }

    /**
     * Stops handler and releases the camera
     */
    public void stop() {
        logger.warn("Stopping handler");
        handlerRunning = false;

        // Try to release camera
        try {
            videoCapture.release();
        } catch (Exception ignored) { }
    }
}
