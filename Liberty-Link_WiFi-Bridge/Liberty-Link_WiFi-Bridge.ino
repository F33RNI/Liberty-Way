/*
* Copyright (C) 2021 Fern H. (aka Pavel Neshumov), Liberty-Link Wifi bridge
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
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

#include <ESP8266WiFi.h>
#include <WiFiUdp.h>

// UART baud rate
const uint32_t UART_BAUDRATE PROGMEM = 115200;

// UDP to serial buffer size
const uint16_t RX_BUFFER_SIZE PROGMEM = 1024;

// Serial to UDP buffer size
const uint16_t TX_BUFFER_SIZE PROGMEM = 1024;

// Time to wait (in microseconds) before starting transmission to collect all data into one packet
// Set to zero to transmit characters as they arrive instead of buffering them into one packet
const uint16_t PACKET_TIMEOUT PROGMEM = 4500;

// UDP port (for AP and STA modes)
const uint16_t UDP_PORT PROGMEM = 9999;

// ESP Mode
// In MODE_AP: device connects directly to ESP
// In MODE_STA: ESP connects to WiFi router
//#define MODE_AP
#define MODE_STA


#ifdef MODE_AP
// Credentials for ESP Access Point
const char* AP_SSID PROGMEM = "Liberty-X";
const char* AP_PSK PROGMEM = "libertylinkwifi";

// IP and mask for ESP Access Point
IPAddress AP_IP(192, 168, 0, 1);
IPAddress AP_NETMASK(255, 255, 255, 0);
IPAddress BROADCAST_IP(192, 168, 0, 255);
#endif


#ifdef MODE_STA
// Credentials for ESP Station mode (router's ssid and password)
const char* STA_SSID = "ROUTER'S SSID";
const char* STA_PSK = "ROUTER'S PASSWORD";

// Local IP, gateway, mask and DNS for ESP Station mode
IPAddress STA_LOCAL_IP(192, 168, 9, 184); // (192, 168, 1, 184)
IPAddress STA_GATEWAY(192, 168, 9, 1); // (192, 168, 1, 1)
IPAddress STA_SUBNET(255, 255, 255, 0);
IPAddress BROADCAST_IP(192, 168, 9, 255); // (192, 168, 1, 255)
IPAddress STA_DNS(8, 8, 8, 8);
#endif


uint8_t udp_to_uart_buffer[RX_BUFFER_SIZE];
uint16_t udp_to_uart_buffer_position;
uint8_t uart_to_udp_buffer[TX_BUFFER_SIZE];
uint16_t uart_to_udp_buffer_position;

WiFiUDP udp;
IPAddress remote_ip;

int incoming_packet_size;

unsigned long uart_to_udp_last_time;


void setup() {
    // Setup led pin as output
    pinMode(LED_BUILTIN, OUTPUT);

    // Initialize serial port
    Serial.begin(UART_BAUDRATE);
    delay(200);

#ifdef MODE_AP 
    // AP mode (device connects directly to ESP) (no router)
    WiFi.mode(WIFI_AP);
    // Configure ip address for softAP 
    WiFi.softAPConfig(AP_IP, AP_IP, AP_NETMASK);
    // Configure ssid and password for softAP
    WiFi.softAP(AP_SSID, AP_PSK);
#endif


#ifdef MODE_STA
    // STATION mode (ESP connects to router and gets an IP)
    WiFi.mode(WIFI_STA);
    WiFi.begin(STA_SSID, STA_PSK);
    WiFi.config(STA_LOCAL_IP, STA_GATEWAY, STA_SUBNET, STA_DNS);
    // Wait for connection
    while (WiFi.status() != WL_CONNECTED) {
        digitalWrite(LED_BUILTIN, 1);
        delay(50);
        digitalWrite(LED_BUILTIN, 0);
        delay(50);
    }
    WiFi.setAutoReconnect(true);
    digitalWrite(LED_BUILTIN, 1);
#endif 

    // Start UDP server
    udp.begin(UDP_PORT);

    // Flush serial and udp buffers
    Serial.flush();
    udp.flush();
}


void loop() {
    // Read data from UDP and send it via serial port
    incoming_packet_size = udp.parsePacket();
    if (incoming_packet_size > 0) {
        // If there is data available
        // Store the ip of the remote device
        remote_ip = udp.remoteIP();

        // Read data into buffer
        udp.read(udp_to_uart_buffer, RX_BUFFER_SIZE);

        // Send data to UART
        Serial.write(udp_to_uart_buffer, incoming_packet_size);
    }

    // Read data from serial port and store it into the buffer
    if (Serial.available()) {
        // Push data into the buffer
        uart_to_udp_buffer[uart_to_udp_buffer_position] = Serial.read();
        if (uart_to_udp_buffer_position < TX_BUFFER_SIZE - 1)
            uart_to_udp_buffer_position++;

        // Store last byte time
        uart_to_udp_last_time = micros();
    }

    // Send buffer via UDP after packet timeout (if there is no new data)
    if (uart_to_udp_buffer_position > 0 && micros() - uart_to_udp_last_time >= PACKET_TIMEOUT) {
        // Remote IP and Port if known or use broadcast IP
        if (remote_ip.isSet())
            udp.beginPacket(remote_ip, UDP_PORT);
        else
            udp.beginPacket(BROADCAST_IP, UDP_PORT);

        udp.write(uart_to_udp_buffer, uart_to_udp_buffer_position);
        udp.endPacket();
        uart_to_udp_buffer_position = 0;
    }
}