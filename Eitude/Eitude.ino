/*
 * Copyright (C) 2021 Fern Hertz (Pavel Neshumov), Eitude AMLS Platform controller
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

// External libraries
#include <Wire.h>
#include <Adafruit_NeoPixel.h>
#include <EtherCard.h>
#include <IPAddress.h>

// System config, constants and variables
#include "config.h"
#include "constants.h"
#include "datatypes.h"

// External library objects
#ifdef WS_LEDS
Adafruit_NeoPixel ws_leds = Adafruit_NeoPixel(3, STATUS_STRIP_PIN, NEO_GRB + NEO_KHZ800);
#endif

void setup()
{
	// Store packet ending in tx_buffer
	tx_buffer[17] = PACKET_SUFFIX_1;
	tx_buffer[18] = PACKET_SUFFIX_2;

	// Hardware setup
	// Power pins for LDR module
	pinMode(A2, OUTPUT);
	digitalWrite(A2, 0);
	pinMode(A3, OUTPUT);
	digitalWrite(A3, 1);
	pinMode(STATUS_STRIP_PIN, OUTPUT);
	pinMode(LIGHTS_PIN, OUTPUT);

	// Init leds strip
#ifdef WS_LEDS
	ws_setup();
#endif

	// Initialize I2C
	Wire.begin();

	// Wait for hardware initialization
	delay(200);

	// Communication setup
#ifdef UDP_PORT
	// Init ENC28J60 moduule
	if (ether.begin(sizeof Ethernet::buffer, MAC, SS) == 0) {
		// Failed to access Ethernet controller
		error = 1;
		while (error != 0) {
			// Show curent error
			leds_error_signal();

			// Simulate main loop
			delayMicroseconds(LOOP_PERIOD);
		}
	}

	// Set DHCP address
	//ether.dhcpSetup();
	// Set static address
	ether.staticSetup(STATIC_IP, GATEWAY_IP, DNS_IP, MASK);

	if (ether.myip[0] != STATIC_IP[0] || ether.myip[1] != STATIC_IP[1]
		|| ether.myip[2] != STATIC_IP[2] || ether.myip[3] != STATIC_IP[3]) {
		// Failed to setup static IP
		error = 2;
		while (error != 0) {
			// Show curent error
			leds_error_signal();

			// Simulate main loop
			delayMicroseconds(LOOP_PERIOD);
		}
	}

	// Start listeting on udp_port
	ether.udpServerListenOnPort(&udp_receive_data, UDP_PORT);
#else
	// Serial port setup
	COMMUNICATION_SERIAL.begin(COMMUNICATION_BAUDRATE);
#endif

	// Setup barometer
#ifdef BAROMETER
	barometer_setup();
#endif

	// Setup LUX meter
#ifdef LUX_METER
	lux_meter_setup();
#endif

	// Setup GPS
#ifdef GPS
	gps_setup();
#endif

	// Flush serial port
#ifndef UDP_PORT
	COMMUNICATION_SERIAL.flush();
#endif

	// Reset GPS
#ifdef GPS
	gps_reset_flag = 1;
#endif

	// Store loop time
	loop_timer = micros();
}

void loop()
{
	// Receive data from UDP or serial port
#ifdef UDP_PORT
	ether.packetLoop(ether.packetReceive());
#else
	serial_receive_data();
#endif

	// Read and parse data from GPS module
#ifdef GPS
	gps_read();
	gps_handler();
#endif

	// Read and parse data from barometer
#ifdef BAROMETER
	barometer_handler();
#endif

	// Read illumination
	lux_meter();

	// Turn on/off backlight
	backlight();

	// Light up WS2812 LEDs
	ws_light();

#ifndef UDP_PORT
	// Send tx_buffer with detecting timeout
	if (tx_flag)
		transmit_data();
#endif

	// Check loop time
	if (micros() - loop_timer > MAX_ALLOWED_LOOP_PERIOD)
		error = 5;

#ifdef UDP_PORT
	// Send tx_buffer without detecting timeout
	if (tx_flag)
		transmit_data();
#ifdef GPS
	// New connection -> flush GPS on timeout
	if (micros() - loop_timer > MAX_ALLOWED_LOOP_PERIOD)
		gps_reset_flag = 1;
#endif
#endif

	// Wait minimum loop time
	while (micros() - loop_timer < LOOP_PERIOD);
	loop_timer = micros();
}