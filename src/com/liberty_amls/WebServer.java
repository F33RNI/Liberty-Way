/*
 * Copyright (C) 2021 Fern H. (Pavel Neshumov), Liberty-Way Landing System Project
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

import flak.App;
import flak.Flak;
import flak.Response;
import flak.annotations.Post;
import flak.annotations.Route;
import flak.plugin.resource.FlakResourceImpl;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class WebServer {
    private final Logger logger = Logger.getLogger(this.getClass().getSimpleName());
    private final SettingsContainer settingsContainer;
    private App app;
    private final WebAPI webAPI;
    private final String hostName;
    private final int serverPort;
    private final int videoPort;

    /**
     * This class creates a server (based on the Flak library) to control the landing process
     * @param hostName server IP
     * @param serverPort server (controller) Port
     * @param videoPort http video stream port
     */
    public WebServer(String hostName, int serverPort, int videoPort, SettingsContainer settingsContainer) {
        this.videoPort = videoPort;
        this.serverPort = serverPort;
        this.hostName = hostName;
        this.settingsContainer = settingsContainer;
        this.webAPI = new WebAPI(hostName, videoPort, settingsContainer);
    }

    /**
     * Redirects to video stream
     */
    @Route("/video_feed")
    public void videoFeed(Response resp) {
        // Redirect hostName:serverPort/video_feed to hostName:videoPort
        resp.redirect(app.getRootUrl().replace(
                app.getRootUrl().split(":")[app.getRootUrl().split(":").length - 1],
                String.valueOf(videoPort)));
    }

    /**
     * Redirects POST request to WebAPI class
     */
    @Post
    @Route("/api")
    public Response api(Response response) throws IOException {
        return webAPI.newRequest(response);
    }

    /**
     * Loads main page (setup, controller or aborted)
     */
    @Route("/")
    public String index() {
        String content;
        if (webAPI.isAborted())
            // Load connection_closed page if aborted flag provided
            content = FileWorkers.loadString(settingsContainer.webTemplatesFolder + "/aborted.html");
        else if (webAPI.isControllerRunning())
            // Load controller page if it was started
            content = FileWorkers.loadString(settingsContainer.webTemplatesFolder + "/controller.html");
        else
            // Load index (main) page if controller is not running
            content = FileWorkers.loadString(settingsContainer.webTemplatesFolder + "/index.html");

        if (webAPI.isAborted())
            // Just return page if aborted flag provided
            return content;

        if (!webAPI.isControllerRunning()) {
            // index.html
            // Scan available serial ports
            StringBuilder serialPortsSelector = new StringBuilder();
            List<String> serialPorts = SerialHandler.getPortNames();
            logger.info("Discovered serial ports: " + serialPorts);

            // Creates a drop-down list (select html tag)
            for (String serialPort : serialPorts) {
                serialPortsSelector.append("<option value=\"")
                        .append(serialPort)
                        .append("\">")
                        .append(serialPort)
                        .append("</option>");
            }

            // Put ports into html page
            content = content.replace("{{ platform_ports }}", serialPortsSelector.toString());
            content = content.replace("{{ link_ports }}", serialPortsSelector.toString());
        }
        return content;
    }

    /**
     * Starts Flak server
     */
    public void start() {
        try {
            logger.info("Trying to start the server...");
            // Create Flak server with provided port and ip
            app = Flak.createHttpApp(serverPort);
            app.getServer().setHostName(hostName);

            // Add static directory (for resources)
            Path dir = Paths.get(settingsContainer.webResourcesFolder);
            new FlakResourceImpl(app).servePath("/",
                    dir.toString(),
                    getClass().getClassLoader(),
                    false);

            // Start the server from this class
            app.scan(this);
            app.start();
            logger.info("The server is running on " + hostName + ":" + serverPort);
        } catch (Exception e) {
            logger.error("Error starting the server!", e);
            System.exit(1);
        }
    }

    /**
     * Stops Flak server
     */
    public void stop() {
        logger.info("Stopping the server");
        app.stop();
    }
}
