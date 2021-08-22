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

const mapboxToken = 'pk.eyJ1IjoiLWZlcm4tIiwiYSI6ImNrczRzNGE2ZDB2MzAycG5qdGw4YXhkaHgifQ.VwR0dc3BeVNMMvSOJmq06A';
const mapboxStyle = 'mapbox://styles/-fern-/cks58glcmc69x18rzqxtvfdzh';
const telemetryUpdateTime = 500;
const initialZoomLevel = 18;

const telemetryXMLHttp = new XMLHttpRequest();

let telemetryPackets = 0;

let map = null;
let homePin = null;
let isHomePinSet = false;
let dronePin = null;
let isDronePinSet = false;
let backgroundStreamImg = null;

window.onload = onLoad;

/**
 * Initializes the map, telemetry reception and determines the view (camera / map) when the page is loaded.
 */
function onLoad() {
	/* Initialize the map */
	try {
		mapboxgl.accessToken = mapboxToken;
		map = new mapboxgl.Map({
			container: 'map-container',
			style: mapboxStyle,
			zoom: -1
		});
		homePin = new mapboxgl.Marker({
			color: '#99daff',
			draggable: false
		}).setPopup(new mapboxgl.Popup()
			.setText('Platform location')
			.addTo(map));
		dronePin = new mapboxgl.Marker({
			color: "#e34f97",
			draggable: false
		}).setPopup(new mapboxgl.Popup()
			.setText('Drone location')
			.addTo(map));
	} catch (err) {
		map = null;
		alert("Error loading map!\n" + err.message + "\nCheck your internet connection and reload the page");
	}

	checkVideoOrMap();

	/* Cyclically sends request and receives telemetry */
	telemetryXMLHttp.onreadystatechange = function () {
		if (this.readyState === 4) {
			if (this.status === 200) {
				const response = JSON.parse(this.responseText);
				if (response.status.toLowerCase() === "ok") {
					printTelemetry(response.telemetry);
					telemetryRefresh(true);
				} else {
					console.log("Wrong response!\nServer response: " + this.responseText);
					telemetryRefresh(false);
				}

			} else {
				console.log("Error sending POST request!\nHTTP Status: " + this.status
					+ "\nServer response: " + this.responseText);
				telemetryRefresh(false);
			}
		}
	};
	requestTelemetry();
}

/**
 * Checks if the video stream is available,
 * if yes, switches the view to the camera, if not - to the map
 */
function checkVideoOrMap() {
	const xmlHTTP = new XMLHttpRequest();
	xmlHTTP.onreadystatechange = function () {
		if (this.readyState === 4 && this.status === 200) {
			const response = JSON.parse(this.responseText);
			if (response.status.toLowerCase() === "ok") {
				if (response.video.toLowerCase() === "enabled")
					disableMapEnableCamera();
				else
					enableMapDisableCamera();
			} else
				alert("Error checking video stream status!\nPlease reload the page");
		}
	};

	/* Send check_stream API request */
	xmlHTTP.open("POST", "/api", true);
	xmlHTTP.setRequestHeader("Content-Type", "application/json");
	xmlHTTP.send(JSON.stringify({"action": "check_stream"}));
}

/**
 * Switches view to the camera
 */
function disableMapEnableCamera() {
	/* Hide map container */
	document.getElementById("map-container").style.visibility = "hidden";

	/* Create background image */
	backgroundStreamImg = document.createElement('div');
	backgroundStreamImg.innerHTML = "<img class=\"background-video-stream-blurred\" src=\"/video_feed\" " +
		"alt=\"Platform camera\">" +
		"<img class=\"background-video-stream\" src=\"/video_feed\" alt=\"Platform camera\">";
	document.getElementById("stream-container").appendChild(backgroundStreamImg);
	document.getElementById("stream-container").style.visibility = "visible";
}

/**
 * Switches view to the map
 */
function enableMapDisableCamera() {
	/* Show map container */
	document.getElementById("map-container").style.visibility = "visible";

	/* Delete image stream */
	if (backgroundStreamImg != null)
		backgroundStreamImg.remove();
	document.getElementById("stream-container").style.visibility = "hidden";
}

/**
 * Sends telemetry API request
 */
function requestTelemetry() {
	telemetryXMLHttp.open("POST", "/api", true);
	telemetryXMLHttp.setRequestHeader("Content-Type", "application/json");
	telemetryXMLHttp.send(JSON.stringify({"action": "telemetry"}));
}

/**
 * Prints telemetry data on left of the screen
 * @param telemetry JSON telemetry data
 */
function printTelemetry(telemetry) {
	telemetryPackets++;

	/* Number of receiver packets */
	document.getElementById("telemetry-packets").innerHTML = telemetryPackets.toString();

	/* Timeline progress */
	document.getElementById("timeline-progress").style.width = telemetry.progress + "%";

	/* System status */
	document.getElementById("status").innerHTML = telemetry.status;

	/* Distance between drone and platform */
	document.getElementById("distance").innerHTML = telemetry.distance + " m";

	/* Drone telemetry */
	document.getElementById("drone_packets").innerHTML = telemetry.drone_packets;
	document.getElementById("drone_voltage").innerHTML = telemetry.drone_voltage + " V";
	document.getElementById("drone_altitude").innerHTML = telemetry.drone_altitude + " m";
	document.getElementById("drone_satellites").innerHTML = telemetry.drone_satellites;
	document.getElementById("drone_lat").innerHTML = telemetry.drone_lat;
	document.getElementById("drone_lon").innerHTML = telemetry.drone_lon;
	document.getElementById("drone_speed").innerHTML = telemetry.drone_speed + " km/h";

	/* Platform telemetry */
	document.getElementById("platform_packets").innerHTML = telemetry.platform_packets;
	document.getElementById("platform_satellites").innerHTML = telemetry.platform_satellites;
	document.getElementById("platform_lat").innerHTML = telemetry.platform_lat;
	document.getElementById("platform_lon").innerHTML = telemetry.platform_lon;
	document.getElementById("platform_speed").innerHTML = telemetry.platform_speed + " km/h";

	/* Put GPS markers on the map */
	putTelemetryOnMap(telemetry);
}

/**
 * Updates markers on map
 * @param telemetry telemetry data with GPS coordinates
 */
function putTelemetryOnMap(telemetry) {
	if (map !== null) {
		/* Drone marker */
		if (!telemetry.drone_telemetry_lost && telemetry.drone_satellites > 1) {
			dronePin.setLngLat([telemetry.drone_lon, telemetry.drone_lat]);
			if (!isDronePinSet) {
				dronePin.addTo(map);
				map.setZoom(initialZoomLevel);
				map.panTo([telemetry.drone_lon, telemetry.drone_lat]);
				isDronePinSet = true;
			}
		}
		else if (isDronePinSet) {
			dronePin.remove();
			isDronePinSet = false;
		}

		/* Platform marker */
		if (!telemetry.platform_lost && telemetry.platform_satellites > 1) {
			homePin.setLngLat([telemetry.platform_lon, telemetry.platform_lat]);
			if (!isHomePinSet) {
				homePin.addTo(map);
				if (!isDronePinSet) {
					map.setZoom(initialZoomLevel);
					map.panTo([telemetry.platform_lon, telemetry.platform_lat]);
				}
				isHomePinSet = true;
			}
		}
		else if (isHomePinSet) {
			homePin.remove();
			isHomePinSet = false;
		}
	}
}

/**
 * Requests telemetry from Liberty-Way
 * @param connected is connection up?
 */
function telemetryRefresh(connected) {
	document.getElementById("telemetry-status").innerHTML = connected ? "Connected" : "Lost";
	if (!connected) {
		console.log("Telemetry lost! Attempt to retry after 5 seconds");
		setTimeout(requestTelemetry, 5000);
	} else {
		setTimeout(requestTelemetry, telemetryUpdateTime);
	}
}

/**
 * Starts Liberty-Way sequence
 */
function execute() {
	const xmlHTTP = new XMLHttpRequest();
	xmlHTTP.onreadystatechange = function () {
		if (this.readyState === 4 && this.status === 200 && JSON.parse(this.responseText).status.toLowerCase() === "ok")
			document.getElementById("timeline-progress").style.width = "100%";
	};

	/* Send execute API request */
	xmlHTTP.open("POST", "/api", true);
	xmlHTTP.setRequestHeader("Content-Type", "application/json");
	xmlHTTP.send(JSON.stringify({"action": "execute"}));
}

/**
 * Switches the view between camera and platform
 */
function switchView() {
	const xmlHTTP = new XMLHttpRequest();
	xmlHTTP.onreadystatechange = function () {
		if (this.readyState === 4 && this.status === 200) {
			const response = JSON.parse(this.responseText);
			if (response.status.toLowerCase() === "ok") {
				if (response.video.toLowerCase() === "enabled")
					disableMapEnableCamera();
				else
					enableMapDisableCamera();
			}
		}
	};

	/* Send toggle_stream API request */
	xmlHTTP.open("POST", "/api", true);
	xmlHTTP.setRequestHeader("Content-Type", "application/json");
	xmlHTTP.send(JSON.stringify({"action": "toggle_stream"}));
}

/**
 * Exits the application
 */
function abort() {
	const xmlHTTP = new XMLHttpRequest();
	xmlHTTP.onreadystatechange = function() {
		if (this.readyState === 4 && this.status === 200
			&& JSON.parse(this.responseText).status.toLowerCase() === "ok")
			window.location.href = "/";
	};

	/* Send abort API request */
	xmlHTTP.open("POST", "/api", true);
	xmlHTTP.setRequestHeader("Content-Type", "application/json");
	xmlHTTP.send(JSON.stringify({ "action": "abort" }));
}