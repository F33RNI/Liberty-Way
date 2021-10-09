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
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

import java.io.File;
import java.util.Objects;

public class Tester {
    private final Logger logger = Logger.getLogger(this.getClass().getSimpleName());

    private final String testLevel;

    /**
     * This class provides a series of automated tests to be performed to check system compatibility
     * @param testLevel level of test (build, opencv, camera, server, full)
     */
    Tester(String testLevel) {
        this.testLevel = testLevel;
    }

    public void testByLevel() {
        try {
            // Check test level for null
            if (testLevel == null)
                throw new TesterException("Test level cannot be null!");

            // Execute selected test level
            switch (testLevel) {
                case "build":
                    // Check if the app starts successfully
                    logger.info("Performing build tests");
                    if (buildTest())
                        System.exit(0);
                    else
                        System.exit(1);
                    return;

                case "opencv":
                    // Check opencv native library
                    logger.info("Performing opencv native library tests");
                    if (opencvTest())
                        System.exit(0);
                    else
                        System.exit(1);
                    return;

                case "camera":
                    // Test opencv library and cameras
                    logger.info("Performing camera tests");
                    if (cameraTest())
                        System.exit(0);
                    else
                        System.exit(1);
                    return;

                case "server":
                    // Check if the server can be started
                    logger.info("Performing server tests");
                    if (serverTest())
                        System.exit(0);
                    else
                        System.exit(1);
                    return;

                case "full":
                    // Full environmental check
                    logger.info("Performing full environmental tests");
                    if (buildTest() && opencvTest() && cameraTest() && serverTest())
                        System.exit(0);
                    else
                        System.exit(1);
                    return;

                default:
                    // Wrong test level
                    throw new TesterException("Wrong test level provided!");
            }
        } catch (Exception e) {
            // Print error message
            logger.error("Error passing tests!", e);

            // Exit with error
            System.exit(1);
        }
    }

    /**
     * Performs a basic settings check
     */
    public boolean buildTest() {
        // Parse settings from JSON
        logger.info("Attempting to parse settings");
        SettingsContainer testSettingsContainer = new SettingsContainer();
        new SettingsHandler(testSettingsContainer,
                FileWorkers.loadJsonObject("settings.json")).parseSettings();

        // Check if SettingsContainer has cameraMatrixFile and cameraDistortionsFile
        if (testSettingsContainer.cameraMatrixFile.length() > 0 &&
                testSettingsContainer.cameraDistortionsFile.length() > 0) {
            logger.info("Build test passed");
            return true;
        } else
            logger.error("cameraMatrixFile or cameraDistortionsFile is empty. Test failed!");
        return false;
    }

    /**
     * Performs opencv native library test
     */
    public boolean opencvTest() {
        // Load native library (from java-library-path)
        logger.info("Loading OpenCV Native Library");
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // Load watermark.png as Mat object for test
        if (!Objects.requireNonNull(FileWorkers.loadImageAsMat(new File("watermark.png"))).empty()) {
            logger.info("OpenCV Native Library test passed");
            return true;
        } else
            logger.error("Watermark Mat is empty or null. Test failed!");
        return false;
    }

    /**
     * Checks if at least one camera is available in the system
     */
    public boolean cameraTest() {
        // Load native library (from java-library-path)
        logger.info("Loading OpenCV Native Library");
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // Test first 10 cameras
        boolean hasOneCamera = false;
        VideoCapture testVideoCapture = new VideoCapture();
        Mat testMat = new Mat();
        for (int i = 0; i < 10; i++) {
            // Test camera
            boolean captureState;
            try {
                logger.info("Opening VideoCapture " + i + "...");
                testVideoCapture.open(i);
                logger.info("Reading frame...");
                testVideoCapture.read(testMat);
                logger.info("Checking state...");
                captureState = testVideoCapture.isOpened();
                logger.info("Releasing capture...");
                testVideoCapture.release();
                logger.info("Done.");
            } catch (Exception ignored) {
                captureState = false;
            }

            // Print camera info
            if (captureState) {
                logger.info("Camera with index " + i + " can be used");
                hasOneCamera = true;
            }
            else
                logger.warn("Camera with index " + i + " cannot be opened!");
        }

        // The test is passed if at least one camera is available
        if (hasOneCamera) {
            logger.info("Camera test passed");
            return true;
        } else
            logger.error("No cameras available in the system. Test failed!");
        return false;
    }

    public boolean serverTest() {
        // Create settings container and parse app settings
        SettingsContainer settingsContainer = new SettingsContainer();
        SettingsHandler settingsHandler = new SettingsHandler(settingsContainer,
                FileWorkers.loadJsonObject("settings.json"));
        settingsHandler.parseSettings();

        // Start the server with default IP and Port
        logger.info("Starting test server with " +
                "IP: " + settingsContainer.defaultServerHost +
                ", server port: " + settingsContainer.defaultServerPort  +
                ", video port: " + settingsContainer.defaultVideoPort);
        try {
            WebServer webServer = new WebServer(settingsContainer.defaultServerHost,
                    settingsContainer.defaultServerPort,
                    settingsContainer.defaultVideoPort,
                    settingsContainer);
            webServer.start();
            webServer.stop();
            logger.info("Server test passed");
            return true;
        } catch (Exception e) {
            logger.error("Error starting/stopping the server. Test failed!", e);
        }
        return false;
    }
}
