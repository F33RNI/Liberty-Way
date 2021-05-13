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

import org.apache.log4j.Logger;
import org.opencv.core.Mat;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class VideoStream {
    private final Logger logger = Logger.getLogger(this.getClass().getSimpleName());
    private final InetAddress serverIP;
    private final int serverPort;
    private final BufferedImage bufferedImage;
    private final byte[] frameBytes;
    private ServerSocket serverSocket;
    private Socket socket;
    private volatile boolean serverRunning;

    /**
     * This class creates jpeg video stream via http that can be wived in browser
     * @param serverIP InetAddress object (IP of the server)
     * @param serverPort Video stream port (ex. 8080 or 5000)
     */
    public VideoStream(InetAddress serverIP, int serverPort, int frameWidth, int frameHeight) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        bufferedImage = new BufferedImage(frameWidth, frameHeight, BufferedImage.TYPE_3BYTE_BGR);
        frameBytes = new byte[frameWidth * frameHeight * 3];
    }

    /**
     * Writes http header (continuous stream and no headers)
     * @param stream output stream of the serverSocket
     */
    private void writeHeader(OutputStream stream) throws IOException {
        stream.write(("HTTP/1.0 200 OK\r\n" +
                "Connection: close\r\n" +
                "Max-Age: 0\r\n" +
                "Expires: 0\r\n" +
                "Cache-Control: no-store, no-cache, must-revalidate, pre-check=0, post-check=0, max-age=0\r\n" +
                "Pragma: no-cache\r\n" +
                "Content-Type: multipart/x-mixed-replace; " +
                "boundary=" + "stream" + "\r\n" +
                "\r\n" +
                "--" + "stream" + "\r\n").getBytes());
    }

    /**
     * Pushes frame to the page
     */
    public void pushFrame(Mat frame) throws IOException {
        if (frame == null || !serverRunning)
            return;
        try {
            OutputStream outputStream = socket.getOutputStream();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            frame.get(0, 0, frameBytes);
            System.arraycopy(frameBytes, 0,
                    ((DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData(), 0,
                    frameBytes.length);
            ImageIO.write(bufferedImage, "jpg", byteArrayOutputStream);
            byteArrayOutputStream.close();
            byte[] imageBytes = byteArrayOutputStream.toByteArray();
            outputStream.write(("Content-type: image/bmp\r\n" +
                    "Content-Length: " + imageBytes.length + "\r\n" +
                    "\r\n").getBytes());
            outputStream.write(imageBytes);
            outputStream.write(("\r\n--stream\r\n").getBytes());
            outputStream.flush();
        } catch (Exception e) {
            socket = serverSocket.accept();
            writeHeader(socket.getOutputStream());
            logger.warn("Error pushing the frame to the server!");
        }
    }

    /**
     * Create ServerSocket object, writes the headers and sets the serverRunning flag
     */
    public void start() {
        try {
            serverSocket = new ServerSocket(serverPort, 0, serverIP);
            socket = serverSocket.accept();
            writeHeader(socket.getOutputStream());
            serverRunning = true;
        } catch (IOException e) {
            logger.error("Error starting video stream!", e);
        }
    }

    /**
     * Closes sockets and sets the serverRunning flag
     */
    public void stop() {
        try {
            serverRunning = false;
            socket.close();
            serverSocket.close();
        } catch (IOException e) {
            logger.error("Error closing video stream!", e);
        }
    }
}
