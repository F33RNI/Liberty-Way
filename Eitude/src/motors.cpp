/**
 * Copyright (C) 2022 Vladislav Yasnetsky, Eitude AMLS Platform controller
 * This software is part of Liberty Drones Project
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

#include "datatypes.h"
#include "config.h"
#include "motors.h"

/**
 * @brief Initializes stepper motors
 * 
 */
void motors_setup(void) {
    // Initialize end switch pin
    pinMode(PIN_END_SWITCH, INPUT_PULLUP);

    // Add motors to the FastAccelStepper
    engine.init();
    alignment_stepper = engine.stepperConnectToPin(PIN_ALIGNMENT_STEPPER_STEP);
    if (alignment_stepper == NULL) {
        // Failed to connect stepper motor
        error = ERROR_MOTORS_SETUP;
        while (error != 0)
            // Show current error
            status_led();
    }

    // Set enable and direction pins
    alignment_stepper->setDirectionPin(PIN_ALIGNMENT_STEPPER_DIR);
    alignment_stepper->setEnablePin(PIN_ALIGNMENT_STEPPER_EN);

    // Disable stepper motors
    alignment_stepper->disableOutputs();

    // Set speed and acceleration for the stepper motors
    alignment_stepper->setSpeedInHz(STEPPER_SPEED);
    alignment_stepper->setAcceleration(STEPPER_ACCELERATION);
}

/**
 * @brief Opens alignment system
 * 
 */
void motors_home(void) {
    // Check if end switch is not pressed
    if (digitalRead(PIN_END_SWITCH)) {
        // Enable stepper motors
        alignment_stepper->enableOutputs();

        // Start homing
        alignment_stepper->moveByAcceleration(-STEPPER_ACCELERATION);

        // Start timer
        timeout_timer = millis();

        // Turning stepper motor until end switch is pressed
        while (digitalRead(PIN_END_SWITCH)) {
            // Timeout
            if (millis() - timeout_timer >= STEPPER_HOME_TIMEOUT) {
                // Stop and disable motors
                alignment_stepper->forceStop();
                alignment_stepper->disableOutputs();

                // Failed to home
                error = ERROR_MOTORS_HOME;
                while (error != 0)
                    // Show current error
                    status_led();
            }
        }

        // Stop and disable motors
        alignment_stepper->forceStop();
        alignment_stepper->disableOutputs();

        // Reset current position
        alignment_stepper->setCurrentPosition(0);
        delay(100);
    }
}

/**
 * @brief Closes or opens alignment system
 * 
 */
void motors_alignment_system(void) {
    // OPENED stage
    if (alignment_stage == STAGE_OPENED) {
        // Beginning of the closing sequence
        if (!alignment_state) {
            // Enable motors
            alignment_stepper->enableOutputs();

            // Reset position
            alignment_stepper->setCurrentPosition(0);

            // Start closing
            alignment_stepper->move(STEPS_FOR_CLOSE);

            // Switch to closing stage
            alignment_stage = STAGE_CLOSING;
        }
    }

    // CLOSING stage
    else if (alignment_stage == STAGE_CLOSING) {
        // Motors stopped
        if (!alignment_stepper->isRunning()) {
            // Disable motors
            alignment_stepper->disableOutputs();

            // Switch to closed stage
            alignment_stage = STAGE_CLOSED;
        }
    }

    // CLOSED stage
    else if (alignment_stage == STAGE_CLOSED) {
        // Beginning of the opening sequence
        if (alignment_state) {
            // Enable motors
            alignment_stepper->enableOutputs();

            // Reset position
            alignment_stepper->setCurrentPosition(0);

            // Start homing
            alignment_stepper->moveByAcceleration(-STEPPER_ACCELERATION);

            // Start timer
            timeout_timer = millis();

            // Switch to opening stage
            alignment_stage = STAGE_OPENING;
        }
    }

    // OPENING stage
    else if (alignment_stage == STAGE_OPENING) {
        // End switch pressed
        if (!digitalRead(PIN_END_SWITCH)) {
            // Disable motors
            alignment_stepper->disableOutputs();

            // Switch to opened stage
            alignment_stage = STAGE_OPENED;
        }

        // End switch not pressed and timeout reached
        else if (millis() - timeout_timer >= STEPPER_HOME_TIMEOUT) {
            // Stop and disable motors
            alignment_stepper->forceStop();
            alignment_stepper->disableOutputs();

            // Reset position
            alignment_stepper->setCurrentPosition(0);

            // Failed to home
            error = ERROR_MOTORS_HOME;

            // Switch to error stage
            alignment_stage = STAGE_ERROR;
        }
    }

}