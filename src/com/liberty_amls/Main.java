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

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Main {
    private static final String version = "5.0.0";
    public static final Logger logger = Logger.getLogger(Main.class.getSimpleName());

    public static void main(String[] args) {
        // Set the lowest priority for Main and WebServer classes
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

        // Parse the arguments given from the console
        Options options = new Options();
        options.addOption(Option.builder("i")
                .longOpt("ip")
                .hasArg(true)
                .desc("server ip")
                .required(false)
                .build());
        options.addOption(Option.builder("sp")
                .longOpt("server_port")
                .hasArg(true)
                .desc("web server port (0 - 65535)")
                .required(false)
                .build());
        options.addOption(Option.builder("vp")
                .longOpt("video_port")
                .hasArg(true)
                .desc("video stream port (0 - 65535)")
                .required(false)
                .build());
        options.addOption(Option.builder("c")
                .longOpt("color")
                .hasArg(false)
                .desc("write colored logs")
                .required(false)
                .build());
        options.addOption(Option.builder("t")
                .longOpt("test")
                .hasArg(true)
                .desc("execute tests instead of running the application. Levels:" +
                        "\nbuild - check if the app starts successfully" +
                        "\nopencv - check opencv native library" +
                        "\ncamera - test opencv library and cameras" +
                        "\nserver - check if the server can be started" +
                        "\nfull - full environmental check")
                .required(false)
                .build());
        CommandLineParser parser = new DefaultParser();
        try {
            // Setup Log4J Properties
            System.setProperty("current.date.time",
                    new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss").format(new Date()));
            PropertyConfigurator.configure(Main.class.getResource("log4j.properties"));
            CommandLine cmd = parser.parse(options, args);

            // Use colorful logs properties if 'c' argument specified
            if (cmd.hasOption("c")) {
                PropertyConfigurator.configure(Main.class.getResource("log4j_color.properties"));
                logger.info("Color logs format will be used");
            } else {
                logger.info("Default logs format will be used");
            }

            // Print app version
            logger.info("Liberty-Way AMLS Landing Controller. Version: " + version);

            // Tests (without running the application)
            if (cmd.hasOption("t")) {
                // Run application tests
                String testLevel = cmd.getOptionValue("t");
                logger.warn("--test " + testLevel + " argument provided. Running application's tests");
                new Tester(testLevel).testByLevel();
            } else {
                // Create settings container and parse app settings
                SettingsContainer settingsContainer = new SettingsContainer();
                SettingsHandler settingsHandler = new SettingsHandler(settingsContainer,
                        FileWorkers.loadJsonObject("settings.json"));
                settingsHandler.parseSettings();

                // Custom server IP (Default is specified in the settings.json)
                String serverIP = settingsContainer.defaultServerHost;
                if (cmd.hasOption("i")) {
                    serverIP = cmd.getOptionValue("i");
                    logger.info("Server IP argument provided. IP " + serverIP + " will be used");
                }

                // Custom server port (Default is specified in the settings.json)
                int serverPort = settingsContainer.defaultServerPort;
                if (cmd.hasOption("sp")) {
                    serverPort = Integer.parseInt(cmd.getOptionValue("sp"));
                    logger.info("Server Port argument provided. Port " + serverPort + " will be used");
                }

                // Custom server IP (Default is specified in the settings.json)
                int videoPort = settingsContainer.defaultVideoPort;
                if (cmd.hasOption("vp")) {
                    videoPort = Integer.parseInt(cmd.getOptionValue("vp"));
                    logger.info("Video stream port argument provided. Port " + videoPort + " will be used");
                }

                // Start the server with given IP and Port
                WebServer webServer = new WebServer(serverIP, serverPort, videoPort, settingsContainer);
                webServer.start();
            }
        } catch (ParseException | NumberFormatException e) {
            logger.error("Error parsing command-line arguments!");
            HelpFormatter formatter = new HelpFormatter();
            // Print help message if wrong arguments provided
            formatter.printHelp(
                    "java -jar Liberty-Way.jar " +
                            "[-t build/opencv/camera/server/full] [-i <ip>] [-sp <server_port>] [-vp <video_port>] [-c]"
                    , options);
            // Exit because no correct arguments provided
            System.exit(1);
        }
    }
}
