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

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class Main {
    private static final String version = "beta_3.0.0";
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
                .desc("write colored logs.")
                .required(false)
                .build());
        CommandLineParser parser = new DefaultParser();
        try {
            // Setup Log4J Properties
            PropertyConfigurator.configure(Main.class.getResource("log4j.properties"));
            CommandLine cmd = parser.parse(options, args);

            // Print app version
            logger.info("Liberty-Way AMLS Landing Controller. Version: " + version);

            // Create settings container and parse app settings
            SettingsContainer settingsContainer = new SettingsContainer();
            SettingsHandler settingsHandler = new SettingsHandler(settingsContainer,
                    FileWorkers.loadJsonObject("settings.json"));
            settingsHandler.parseSettings();


            // Use colorful logs properties if 'c' argument specified
            if (cmd.hasOption("c")) {
                PropertyConfigurator.configure(Main.class.getResource("log4j_color.properties"));
                logger.info("Color logs format will be used");
            } else {
                logger.info("Default logs format will be used");
            }

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
        } catch (ParseException | NumberFormatException e) {
            logger.error("Error parsing command-line arguments!", e);
            HelpFormatter formatter = new HelpFormatter();
            // Print help message if wrong arguments provided
            formatter.printHelp(
                    "java -jar Liberty-Way.jar [-i <ip>] [-sp <server_port>] [-vp <video_port>] [-c]"
                    , options);
            // Exit because no correct arguments provided
            System.exit(1);
        }
    }
}
