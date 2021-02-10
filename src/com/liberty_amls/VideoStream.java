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
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class VideoStream {
    private final Logger logger = Logger.getLogger(this.getClass().getSimpleName());
    private ServerSocket serverSocket;
    private Socket socket;
    private final InetAddress serverIP;
    private final int serverPort;
    public boolean serverRunning;
    public Mat frame;

    /**
     * This class creates jpeg video stream via http that can be wived in browser
     * @param initialFrame first frame (could be null)
     * @param serverIP InetAddress object (IP of the server)
     * @param serverPort Video stream port (ex. 8080 or 5000)
     */
    public VideoStream(Mat initialFrame, InetAddress serverIP, int serverPort) {
        this.frame = initialFrame;
        this.serverIP = serverIP;
        this.serverPort = serverPort;
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
    public void pushFrame() throws IOException {
        if (frame == null || !serverRunning)
            return;
        try {
            OutputStream outputStream = socket.getOutputStream();
            BufferedImage bufferedImage = matToBufferedImage(frame);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "jpg", byteArrayOutputStream);
            byte[] imageBytes = byteArrayOutputStream.toByteArray();
            byteArrayOutputStream.close();
            outputStream.write(("Content-type: image/jpeg\r\n" +
                    "Content-Length: " + imageBytes.length + "\r\n" +
                    "\r\n").getBytes());
            outputStream.write(imageBytes);
            String boundary = "stream";
            outputStream.write(("\r\n--" + boundary + "\r\n").getBytes());
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

    /**
     * Converts Mat to BufferedImage
     * @param image OpenCV Mat
     * @return Encoded in JPEG BufferedImages
     */
    private BufferedImage matToBufferedImage(Mat image) throws IOException {
        MatOfByte bytes_mat = new MatOfByte();
        Imgcodecs.imencode(".jpg", image, bytes_mat);
        byte[] bytes = bytes_mat.toArray();
        InputStream in = new ByteArrayInputStream(bytes);
        return ImageIO.read(in);
    }
}
