/*
 * Copyright (C) 2021 Fern Hertz (Pavel Neshumov), Liberty-Way Landing System Project
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
 *
 * IT IS STRICTLY PROHIBITED TO USE THE PROJECT (OR PARTS OF THE PROJECT / CODE)
 * FOR MILITARY PURPOSES. ALSO, IT IS STRICTLY PROHIBITED TO USE THE PROJECT (OR PARTS OF THE PROJECT / CODE)
 * FOR ANY PURPOSE THAT MAY LEAD TO INJURY, HUMAN, ANIMAL OR ENVIRONMENTAL DAMAGE.
 * ALSO, IT IS PROHIBITED TO USE THE PROJECT (OR PARTS OF THE PROJECT / CODE) FOR ANY PURPOSE THAT
 * VIOLATES INTERNATIONAL HUMAN RIGHTS OR HUMAN FREEDOM.
 * BY USING THE PROJECT (OR PART OF THE PROJECT / CODE) YOU AGREE TO ALL OF THE ABOVE RULES.
 */

package com.liberty_amls;

import org.opencv.core.Point;

public class PositionContainer {
    public double x, y, z, yaw;
    public double setpointX, setpointY, setpointAbsX, setpointAbsY, setpointZ, setpointYaw;
    public double entryZ;
    // Camera frame coordinates
    public Point frameSetpoint;
    public Point frameCurrent;
    public int ddcX, ddcY, ddcZ, ddcRoll, ddcPitch, ddcYaw;
    // 0 - WAIT, 1 - MKWT, 2 - WAYP, 3 - STAB, 4 - LAND, 5 - PREV, 6 - LOST, 7 - DONE
    public int status;
    public int distance;
    public boolean isFrameNormal;

    /**
     * This class stores current position, corrections and state of the drone
     */
    PositionContainer() {
        // Initialize variables
        x = 0;
        y = 0;
        z = 0;
        yaw = 0;
        setpointX = 0;
        setpointY = 0;
        setpointAbsX = 0;
        setpointAbsY = 0;
        setpointZ = 0;
        setpointYaw = 0;
        entryZ = 0;
        frameSetpoint = new Point(0, 0);
        frameCurrent = new Point(0, 0);
        ddcX = 1500;
        ddcY = 1500;
        ddcZ = 1500;
        ddcRoll = 1500;
        ddcPitch = 1500;
        ddcYaw = 1500;
        status = 0;
        distance = 0;
        isFrameNormal = false;
    }

    /**
     * Sets setpoints of the PID controllers
     */
    public void setSetpoints(double setpointX, double setpointY, double setpointZ, double setpointYaw) {
        this.setpointX = setpointX;
        this.setpointY = setpointY;
        this.setpointAbsX = setpointX;
        this.setpointAbsY = setpointY;
        this.setpointZ = setpointZ;
        this.setpointYaw = setpointYaw;
    }

    /**
     * @return current status as String
     */
    public String getStatusString() {
        switch (status) {
            case 1:
                return "MKWT";
            case 2:
                return "WAYP";
            case 3:
                return "STAB";
            case 4:
                return "LAND";
            case 5:
                return "PREV";
            case 6:
                return "LOST";
            case 7:
                return "DONE";
            default:
                return "WAIT";
        }
    }
}
