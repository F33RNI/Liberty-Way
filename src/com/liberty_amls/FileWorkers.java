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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

import static java.awt.image.BufferedImage.TYPE_4BYTE_ABGR;

public class FileWorkers {
    /**
     * Loads file as String
     * @param file path to the file
     * @return String (file's content)
     */
    public static String loadString(String file) {
        Main.logger.info("Loading " + file);
        String content = null;
        StringBuilder contentBuilder = new StringBuilder();
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            String str;
            while ((str = bufferedReader.readLine()) != null) {
                contentBuilder.append(str);
            }
            bufferedReader.close();
            content = contentBuilder.toString();
        } catch (Exception e) {
            Main.logger.error("Error loading " + file, e);
        }
        return content;
    }

    /**
     * Loads file into OpenCV Mat
     * @param file path to the .png file (type: ABGR)
     * @return Mat (image) of type BGRA
     */
    public static Mat loadImageAsMat(File file) {
        // Check if file exists
        if (!file.exists()) {
            Main.logger.error("Error! File " + file.getAbsolutePath() + " not exists");
            return null;
        }

        try {
            // Load image as byte array
            Main.logger.info("Loading " + file.getAbsolutePath());
            BufferedImage bufferedImage = ImageIO.read(file);
            byte[] pixels = ((DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData();

            // Convert from ABGR to BGRA
            byte tempPixel;
            for (int i = 0; i < pixels.length; i += 4) {
                tempPixel = pixels[i];
                pixels[i] = pixels[i + 1];
                pixels[i + 1] = pixels[i + 2];
                pixels[i + 2] = pixels[i + 3];
                pixels[i + 3] = tempPixel;
            }

            // Convert to OpenCV Mat
            Mat image = new Mat(bufferedImage.getHeight(), bufferedImage.getWidth(), CvType.CV_8UC4);
            image.put(0, 0, pixels);
            return image;
        } catch (Exception e) {
            Main.logger.error("Error loading " + file, e);
        }
        return null;
    }

    /**
     * Loads camera matrix from json (the file is specified in the settings)
     * @return 3x3 Mat filled with float (32FC1)
     */
    public static Mat loadCameraMatrix(String file) {
        Main.logger.info("Loading " + file);
        Gson gson = new Gson();
        try (Reader reader = new FileReader(file)) {
            JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);
            Mat cameraMatrix = new Mat(jsonArray.size(),jsonArray.get(0).getAsJsonArray().size(), CvType.CV_32FC1);

            for (int row = 0; row < jsonArray.size(); row++) {
                JsonArray rowArray = jsonArray.get(row).getAsJsonArray();
                for (int col = 0; col < rowArray.size(); col++)
                    cameraMatrix.put(row, col, rowArray.get(col).getAsFloat());
            }
            return cameraMatrix;
        } catch (Exception e) {
            Main.logger.error("Error loading " + file, e);
            System.exit(1);
        }
        return null;
    }

    /**
     * Loads camera distortions from json (the file is specified in the settings)
     * @return 1x3 Mat filled with float (32FC1)
     */
    public static Mat loadCameraDistortions(String file) {
        Main.logger.info("Loading " + file);
        Gson gson = new Gson();
        try (Reader reader = new FileReader(file)) {
            JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);
            Mat cameraDistortions = new Mat(1, jsonArray.size(), CvType.CV_32FC1);

            for (int col = 0; col < jsonArray.size(); col++) {
                cameraDistortions.put(0, col, jsonArray.get(col).getAsFloat());
            }
            return cameraDistortions;
        } catch (Exception e) {
            Main.logger.error("Error loading " + file, e);
            System.exit(1);
        }
        return null;
    }

    /**
     * Load file as JsonObject
     * @param file path to the file
     * @return JsonObject (file's content)
     */
    public static JsonObject loadJsonObject(String file) {
        Main.logger.info("Loading " + file);
        JsonObject pids = new JsonObject();
        try (Reader reader = new FileReader(file)) {
            Gson gson = new Gson();
            pids = gson.fromJson(reader, JsonObject.class);

        } catch (Exception e) {
            Main.logger.error("Error reading " + file, e);
        }
        return pids;
    }

    /**
     * Creates parent directories and opens blackbox .csv file with timestamp name
     * @param blackboxDirectory folder with logs
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static File createBlackboxFile(String blackboxDirectory) throws IOException {
        // File path
        File file = new File(blackboxDirectory + "/liberty-way_"
                + new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss").format(new Date()) + ".csv");
        // Create directories and file if not exist
        file.getParentFile().mkdirs();
        file.createNewFile();
        return file;
    }
}
