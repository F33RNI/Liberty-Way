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

import java.util.ArrayList;

public class SettingsContainer {
    public float markerSize;
    public double maxExposure;
    public double cameraAperture;
    public double cameraISO;
    public double motorsTurnOffHeight;
    public boolean opticalLandingAllowed;
    public int maxMarkerHeight;
    public String pidFile;
    public String cameraMatrixFile;
    public String cameraDistortionsFile;
    public String webResourcesFolder;
    public String webTemplatesFolder;
    public String blackboxFolder;
    public int frameWidth;
    public int frameHeight;
    public boolean disableAutoExposure;
    public boolean disableAutoWB;
    public boolean disableAutoFocus;
    public String defaultServerHost;
    public int defaultServerPort;
    public int defaultVideoPort;
    public boolean videoStreamEnabledByDefault;
    public boolean blackboxEnabled;
    public int serialReconnectTime;
    public int udpTimeout;
    public int telemetryLostTime;
    public int platformLostTime;
    public int platformLightEnableThreshold;
    public int platformLightDisableThreshold;
    public int platformLoopTimer;
    public int fpsMeasurePeriod;
    public int adaptiveThreshConstant;
    public short arucoDictionary;
    public ArrayList<Integer> allowedIDs;
    public double inputFilter;
    public double setpointAlignmentFactor;
    public int allowedLostFrames;
    public double landingDecrement;
    public double allowedLandingRangeXY;
    public double allowedLandingRangeYaw;
    public short minSatellitesNumStart;
    public short minSatellitesNum;
    public double minBatteryVoltageStart;
    public double setpointX;
    public double setpointY;
    public double setpointYaw;
    public byte droneDataSuffix1;
    public byte droneDataSuffix2;
    public byte platformDataSuffix1;
    public byte platformDataSuffix2;
    public short pushOSDAfterFrames;
    public double planetRadius;
    public int maxPlatformSpeed;
    public boolean isGPSPredictionAllowed;
    public int stopPredictionOnDistance;
    public boolean logFPS;
    public boolean logAPIRequests;
}
