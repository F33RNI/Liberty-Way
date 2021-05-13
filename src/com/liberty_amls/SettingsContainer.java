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

import java.util.ArrayList;

public class SettingsContainer {
    public float markerSize;
    public double defaultExposure;
    public double landingAlt;
    public boolean landingAllowed;
    public boolean onlyOpticalStabilization;
    public String pidFile;
    public String cameraMatrixFile;
    public String cameraDistortionsFile;
    public String watermarkFile;
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
    public int telemetryLostTime;
    public int platformLostTime;
    public int platformLightEnableThreshold;
    public int platformLightDisableThreshold;
    public int platformReplyTimeout;
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
    public double setpointX;
    public double setpointY;
    public double setpointYaw;
    public byte dataSuffix1;
    public byte dataSuffix2;
    public short pushOSDAfterFrames;
}
