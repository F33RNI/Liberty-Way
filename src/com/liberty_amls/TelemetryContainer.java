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

public class TelemetryContainer {
    public final GPS gps;
    public boolean telemetryLost;
    public int packetsNumber;
    public int errorStatus, flightMode;
    public double batteryVoltage, temperature;
    public int angleRoll, anglePitch, angleYaw;
    public int startStatus;
    public int altitude, takeoffThrottle;
    public boolean takeoffDetected, headingLock;
    public int linkWaypointStep;
    public double illumination;

    /**
     * This class contains all data from the telemetry
     */
    TelemetryContainer() {
        gps = new GPS();
        telemetryLost = true;
        errorStatus = 0;
        flightMode = 1;
        batteryVoltage = 0.0;
        temperature = 0.0;
        angleRoll = 0;
        anglePitch = 0;
        angleYaw = 0;
        startStatus = 0;
        altitude = 0;
        takeoffThrottle = 1500;
        takeoffDetected = false;
        headingLock = false;
        linkWaypointStep = 0;
        illumination = 0.0;
    }
}
