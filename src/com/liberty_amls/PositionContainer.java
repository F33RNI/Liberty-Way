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

import org.opencv.core.Point;

public class PositionContainer {
    public double x, y, z, yaw;
    public double setpointX, setpointY, setpointAbsX, setpointAbsY, setpointZ, setpointYaw;
    public double entryZ;
    // Camera frame coordinates
    public Point frameSetpoint;
    public Point frameCurrent;
    public int ddcX, ddcY, ddcZ, ddcRoll, ddcPitch, ddcYaw;
    // 0 - IDLE, 1 - STAB, 2 - LAND, 3 - PREV, 4 - LOST, 5 - TKOF, 6 - WAYP, 7 - DONE
    public int status;

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
        status = -1;
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
                return "STAB";
            case 2:
                return "LAND";
            case 3:
                return "PREV";
            case 4:
                return "LOST";
            case 5:
                return "TKOF";
            case 6:
                return "WAYP";
            case 7:
                return "DONE";
            default:
                return "IDLE";
        }
    }
}
