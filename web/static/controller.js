/*
 * Copyright (C) 2021 Fern H. (aka Pavel Neshumov), Liberty-Way Landing System Project
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

// Your MapBox token and style
const mapboxToken = "pk.eyJ1IjoiLWZlcm4tIiwiYSI6ImNrczRzNGE2ZDB2MzAycG5qdGw4YXhkaHgifQ.VwR0dc3BeVNMMvSOJmq06A";
const mapboxStyle = "mapbox://styles/-fern-/cks58glcmc69x18rzqxtvfdzh";

// Initial zoom level of the map (when the first points appear)
const initialZoomLevel = 18;

// Update telemetry data every milliseconds
const telemetryUpdateTime = 500;

// Constants for waypoints
const WAYPOINT_SKIP = 0;
const WAYPOINT_PLATFORM = 1;
const WAYPOINT_FLY = 2;
const WAYPOINT_DESCENT = 3;
const WAYPOINT_PARCEL = 4;
const WAYPOINT_LAND = 5;

// System variables
const telemetryXMLHttp = new XMLHttpRequest();
let telemetry = null;
let telemetryPackets = 0;
let map = null;
let homePin = null;
let isHomePinSet = false;
let dronePin = null;
let isDronePinSet = false;
let backgroundStreamImg = null;
let waypointTempPin = null;
let takeoffDetected = false;
let confirmationInProgress = false;
let actionIfConfirmed = null;
let actionIfCanceled = null;
let actionArgumentIfConfirmed = null;
let actionArgumentIfCanceled = null;
let waypointPins = [];
let geoJson = null;
let addingFromMap = false;

// Execute onLoad() function when the page is opened
window.onload = onLoad;

/**
 * Initializes the map, telemetry reception and determines the view (camera / map) when the page is loaded.
 */
function onLoad() {
	/* Initialize the map */
	try {
		mapboxgl.accessToken = mapboxToken;
		map = new mapboxgl.Map({
			container: "map-container",
			style: mapboxStyle,
			zoom: -1
		});
		homePin = new mapboxgl.Marker({
			color: "#99daff",
			draggable: false
		}).setPopup(new mapboxgl.Popup()
			.setText("Platform")).setLngLat([0, 0]).addTo(map);

		dronePin = new mapboxgl.Marker({
			color: "#e34f97",
			draggable: false
		}).setPopup(new mapboxgl.Popup()
			.setText("Drone")).setLngLat([0, 0]).addTo(map);

		waypointTempPin = new mapboxgl.Marker({
			draggable: true,
			color: "#54e34f"
		}).setPopup(new mapboxgl.Popup()
			.setText("New waypoint")).setLngLat([0, 0]);

		function onDrag() {
			let lat = waypointTempPin.getLngLat().lat;
			if (lat < -90)
				lat += 180;
			else if (lat > 90)
				lat -= 180;

			let lon = waypointTempPin.getLngLat().lng;
			if (lon < -180)
				lon += 360;
			else if (lon > 180)
				lon -= 360;

			document.getElementById("waypoint_map_lat").innerText = lat.toFixed(6).toString();
			document.getElementById("waypoint_map_lon").innerText = lon.toFixed(6).toString();
		}
		waypointTempPin.on("drag", onDrag);

		geoJson = {
			'type': 'FeatureCollection',
			'features': [
				{
					'type': 'Feature',
					'geometry': {
						'type': 'LineString',
						'coordinates': [[0, 0]]
					}
				}
			]
		};

		map.on('load', function () {
			map.addSource('line', {
				'type': 'geojson',
				'data': geoJson
			});
			map.addLayer({
				'id': 'lines',
				'type': 'line',
				'source': 'line',
				'paint': {
					'line-width': 3,
					'line-opacity': 0.8,
					'line-color': '#ed6498'
				}
			});
		});

	} catch (err) {
		map = null;
		cyberAlert("ERROR LOADING MAP",
			"Error message: " + err.message + "\nCheck your internet connection and reload the page");
	}

	checkVideoOrMap();

	/* Cyclically sends request and receives telemetry */
	telemetryXMLHttp.onreadystatechange = function () {
		if (this.readyState === 4) {
			if (this.status === 200) {
				const response = JSON.parse(this.responseText);
				if (response.status.toLowerCase() === "ok") {
					telemetry = response.telemetry;
					takeoffDetected = telemetry.takeoff_detected;
					if ((parseFloat(telemetry.drone_lat) !== 0 || parseFloat(telemetry.drone_lon) !== 0)
						&& (parseFloat(telemetry.platform_lat) !== 0 || parseFloat(telemetry.platform_lon !== 0))
						&& (waypointTempPin !== null && (waypointTempPin.getLngLat().lat === 0
							|| waypointTempPin.getLngLat().lon === 0)))
						waypointTempPin.setLngLat([(parseFloat(telemetry.platform_lon)
							+ parseFloat(telemetry.drone_lon)) / 2,
							(parseFloat(telemetry.platform_lat) + parseFloat(telemetry.drone_lat)) / 2]);

					if (!addingFromMap)
						waypointsUpdate(false);
					printTelemetry();
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
					enableMapDisableCamera(true);
			} else
				cyberAlert("ERROR CHECKING VIDEO",
					"Error checking video stream status! Please reload the page");
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
	if (backgroundStreamImg == null) {
		backgroundStreamImg = document.createElement("div");
		backgroundStreamImg.innerHTML = "<img class=\"background-video-stream-blurred\" src=\"/video_feed\" " +
			"alt=\"Platform camera\">" +
			"<img class=\"background-video-stream\" src=\"/video_feed\" alt=\"Platform camera\">";
		document.getElementById("stream-container").appendChild(backgroundStreamImg);
	}
	document.getElementById("stream-container").style.visibility = "visible";
}

/**
 * Switches view to the map
 */
function enableMapDisableCamera(removeStream) {
	/* Show map container */
	document.getElementById("map-container").style.visibility = "visible";

	/* Delete image stream */
	if (removeStream && backgroundStreamImg != null) {
		backgroundStreamImg.remove();
		backgroundStreamImg = null;
	}
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
 */
function printTelemetry() {
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
	document.getElementById("waypoint_index").innerHTML = (telemetry.waypoint_index + 1).toString() + " | " + telemetry.link_waypoint_step;

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

function waypointsUpdate(updateMenu) {
	const xmlHTTP = new XMLHttpRequest();
	xmlHTTP.onreadystatechange = function () {
		if (this.readyState === 4 && this.status === 200) {
			const response = JSON.parse(this.responseText);
			if (response.status.toLowerCase() === "ok") {

				const waypoints = response.waypoints;



				if (updateMenu) {
					const scrollContainer = document.getElementById("scroll-container");
					const scrollElements = document.getElementsByClassName("scroll-element");
					while (scrollElements[0]) {
						scrollElements[0].parentNode.removeChild(scrollElements[0]);
					}

					/* Set ID of new waypoint */
					document.getElementById("add_waypoint_text").innerText = "ADD WAYPOINT "
						+ (waypoints.length + 1);
					document.getElementById("edit_waypoint_text").innerText = "ADD WAYPOINT "
						+ (waypoints.length + 1);
					if (waypoints.length > 0) {
						let waypointElement;
						for (let i = 0; i < waypoints.length; i++) {
							waypointElement = document.createElement("div");
							waypointElement.className = "scroll-element";

							let labelsText = document.createElement("p");
							labelsText.className = "labels-text";
							if (waypoints[i].api > WAYPOINT_SKIP) {
								labelsText.innerHTML = "<a>" + (i + 1) + ": ";
								if (waypoints[i].api !== WAYPOINT_PLATFORM)
									labelsText.innerHTML += waypoints[i].lat + ", " + waypoints[i].lon;

								switch (waypoints[i].api) {
									case WAYPOINT_PLATFORM:
										labelsText.innerHTML += " PLATFORM</a>";
										break;
									case WAYPOINT_FLY:
										labelsText.innerHTML += " JUST FLY</a>";
										break;
									case WAYPOINT_DESCENT:
										labelsText.innerHTML += " DESCENT</a>";
										break;
									case WAYPOINT_PARCEL:
										labelsText.innerHTML += " PARCEL</a>";
										break;
									case WAYPOINT_LAND:
										labelsText.innerHTML += " LAND HERE</a>";
										break;
									default:
										labelsText.innerHTML += "</a>";
										break;
								}
							}
							else
								labelsText.innerHTML = "<a>" + (i + 1) + ": SKIP</a>";

							let deleteButton = document.createElement("div");
							deleteButton.className = "button red small";
							deleteButton.setAttribute("data-augmented-ui","br-clip both");
							deleteButton.style.width = "4em";
							deleteButton.style.marginLeft = "1em";
							deleteButton.addEventListener("click",
								function() { deleteWaypoint(i); }, false);

							deleteButton.innerHTML = "<a>DELETE</a>";

							waypointElement.appendChild(labelsText);
							waypointElement.appendChild(deleteButton);



							scrollContainer.appendChild(waypointElement);
						}
						waypointElement.after(document.getElementById("scroll-buttons"));
						//document.getElementById("scroll-buttons").after(waypointElement);
					}
				}

				geoJson.features[0].geometry.coordinates = [];
				if (waypointPins.length > 0) {
					try {
						for (let i = 0; i < waypointPins.length; i++) {
							waypointPins[i].remove();
							waypointPins.splice(i, 1);
						}
					} catch (e) {
					}
				}
				if (waypoints.length > 0) {
					for (let i = 0; i < waypoints.length; i++) {
						if (waypoints[i].api > WAYPOINT_SKIP) {
							if (waypoints[i].api !== WAYPOINT_PLATFORM) {
								let waypointPin = new mapboxgl.Marker({
									draggable: false,
									color: "#fafa64"
								}).setPopup(new mapboxgl.Popup()
									.setText("Waypoint " + (i + 1))).setLngLat([waypoints[i].lon, waypoints[i].lat])
									.addTo(map);
								waypointPins.push(waypointPin);
								geoJson.features[0].geometry.coordinates.push([waypoints[i].lon, waypoints[i].lat]);
							}
							else if (telemetry !== null
								&& (telemetry.platform_lat !== "0" || telemetry.platform_lon !== "0")
								&& (parseFloat(telemetry.platform_lat) !== 0
									|| parseFloat(telemetry.platform_lon) !== 0))
								geoJson.features[0].geometry.coordinates.push([parseFloat(telemetry.platform_lon),
									parseFloat(telemetry.platform_lat)]);
						}
					}
				} else
					geoJson.features[0].geometry.coordinates = [[0, 0]];
				map.getSource('line').setData(geoJson);
			}
		}
	};

	/* Send get_waypoints API request */
	xmlHTTP.open("POST", "/api", true);
	xmlHTTP.setRequestHeader("Content-Type", "application/json");
	xmlHTTP.send(JSON.stringify({"action": "get_waypoints"}));
}


/**
 * Opens waypoints menu
 */
function menu() {
	waypointsUpdate(true);

	/* Reset selections */
	document.getElementById("manual_action").selectedIndex = null;
	document.getElementById("add_waypoint").selectedIndex = null;

	/* Disable other blocks */
	document.getElementById("from_map_menu").style.display = "none";
	document.getElementById("set_manually_menu").style.display = "none";
	document.getElementById("add_waypoint_menu").style.display = "none";

	/* Enable menu and overlay blocks */
	document.getElementById("menu").style.display = "block";
	document.getElementById("overlay-container").style.display = "block";
}


function createWaypoint() {
	document.getElementById("menu").style.display = "none";
	document.getElementById("add_waypoint_menu").style.display = "block";
	document.getElementById("add_waypoint").selectedIndex = null;
}

function deleteWaypoint(id) {
	if (confirmationInProgress) {
		/* Refresh menu on result */
		const xmlHTTP = new XMLHttpRequest();
		xmlHTTP.onreadystatechange = function () {
			if (this.readyState === 4)
				menu();
		};

		/* Send delete_waypoint API request */
		xmlHTTP.open("POST", "/api", true);
		xmlHTTP.setRequestHeader("Content-Type", "application/json");
		xmlHTTP.send(JSON.stringify({"action": "delete_waypoint", "index": id}));
	}
	else {
		if (takeoffDetected)
			askConfirmation("Drone in flight. Are you sure you want to skip this waypoint?",
				"SKIP", "CANCEL", false,
				deleteWaypoint, id, null, null);
		else
			askConfirmation("Are you sure you want to delete this waypoint?",
				"DELETE", "CANCEL", false,
				deleteWaypoint, id, null, null);
	}
}

function addWaypoint(option) {
	document.getElementById("add_waypoint_note").innerText = "";
	document.getElementById("apply-platform").style.display = "none";

	switch (option) {
		case ("manual"):
			document.getElementById("add_waypoint_menu").style.display = "none";
			document.getElementById("set_manually_menu").style.display = "block";
			document.getElementById("manual_action").selectedIndex = null;
			break;
		case ("map"):
			addingFromMap = true;
			document.getElementById("add_waypoint_menu").style.display = "none";
			enableMapDisableCamera(false);
			document.getElementById("map-container").style.zIndex = "70";
			document.getElementById("from_map_menu").style.display = "block";
			waypointTempPin.addTo(map);
			break;
		case ("drone"):
			if (telemetry !== null && !telemetry.drone_telemetry_lost && telemetry.drone_satellites > 1
				&& (telemetry.drone_lon !== 0 || telemetry.drone_lat !== 0)) {
				document.getElementById("manual_lat").value =
					parseFloat(telemetry.drone_lat).toFixed(6);
				document.getElementById("manual_lon").value =
					parseFloat(telemetry.drone_lon).toFixed(6);
				document.getElementById("add_waypoint_menu").style.display = "none";
				document.getElementById("set_manually_menu").style.display = "block";
				document.getElementById("manual_action").selectedIndex = null;
			} else
				document.getElementById("add_waypoint_note").innerText
					= "Drone GPS is not available!"
			break;
		case ("platform"):
			if (telemetry !== null && !telemetry.platform_lost && telemetry.platform_satellites > 1
				&& (telemetry.platform_lon !== 0 || telemetry.platform_lat !== 0)) {
				document.getElementById("apply-platform").style.display = "inline-block";
			}
			else
				document.getElementById("add_waypoint_note").innerText
					= "Platform GPS is not available!"
			break;
		default:
			break;
	}

}

function applyPlatform() {
	/* Refresh menu on result */
	const xmlHTTP = new XMLHttpRequest();
	xmlHTTP.onreadystatechange = function () {
		if (this.readyState === 4)
			menu();
	};

	/* Send add_waypoint API request */
	xmlHTTP.open("POST", "/api", true);
	xmlHTTP.setRequestHeader("Content-Type", "application/json");
	xmlHTTP.send(JSON.stringify({"action": "add_waypoint", "api" : WAYPOINT_PLATFORM,
		"lat" : telemetry.platform_lat.toString(),
		"lon" : telemetry.platform_lon.toString()}));
}

function applyManual() {
	/* Refresh menu on result */
	const xmlHTTP = new XMLHttpRequest();
	xmlHTTP.onreadystatechange = function () {
		if (this.readyState === 4)
			menu();
	};

	/* Convert selected action to API's integer value */
	let manualAction = document.getElementById("manual_action").value;
	let manualAPI;
	switch (manualAction) {
		case ("fly"):
			manualAPI = WAYPOINT_FLY;
			break;
		case ("descent"):
			manualAPI = WAYPOINT_DESCENT;
			break;
		case ("parcel"):
			manualAPI = WAYPOINT_PARCEL;
			break;
		case ("land"):
			manualAPI = WAYPOINT_LAND;
			break;
		default:
			manualAPI = WAYPOINT_SKIP;
			break;
	}

	/* Send add_waypoint API request */
	xmlHTTP.open("POST", "/api", true);
	xmlHTTP.setRequestHeader("Content-Type", "application/json");
	xmlHTTP.send(JSON.stringify({"action": "add_waypoint", "api" : manualAPI,
		"lat" : document.getElementById("manual_lat").value,
		"lon" : document.getElementById("manual_lon").value}));
}

function applyFromMap() {
	let lat = waypointTempPin.getLngLat().lat;
	if (lat < -90)
		lat += 180;
	else if (lat > 90)
		lat -= 180;

	let lon = waypointTempPin.getLngLat().lng;
	if (lon < -180)
		lon += 360;
	else if (lon > 180)
		lon -= 360;

	document.getElementById("manual_lat").value = lat.toFixed(6);
	document.getElementById("manual_lon").value = lon.toFixed(6);
	closeFromMap();
	addWaypoint("manual");
	addingFromMap = false;
}

function closeWaypoint() {
	menu();
}

function closeFromMap() {
	document.getElementById("manual_action").selectedIndex = null;
	document.getElementById("add_waypoint").selectedIndex = null;
	document.getElementById("from_map_menu").style.display = "none";
	waypointTempPin.remove();
	document.getElementById("map-container").style.zIndex = "0";
	document.getElementById("add_waypoint_menu").style.display = "block";
	checkVideoOrMap();
	addingFromMap = false;
}

function closeManual() {
	document.getElementById("manual_action").selectedIndex = null;
	document.getElementById("add_waypoint").selectedIndex = null;
	document.getElementById("set_manually_menu").style.display = "none";
	document.getElementById("add_waypoint_menu").style.display = "block";
}

function closeMenu() {
	/* Reset selections */
	document.getElementById("manual_action").selectedIndex = null;
	document.getElementById("add_waypoint").selectedIndex = null;

	/* Disable all blocks */
	document.getElementById("from_map_menu").style.display = "none";
	document.getElementById("set_manually_menu").style.display = "none";
	document.getElementById("add_waypoint_menu").style.display = "none";
	document.getElementById("menu").style.display = "none";
	document.getElementById("overlay-container").style.display = "none";
}

function landNow() {
	if (confirmationInProgress) {
		simpleMenuRequest("land");
	} else
		askConfirmation("Are you sure you want to start the auto-landing sequence now? " +
			"\nThe current flight will be canceled and the drone will begin descending",
			"LAND", "CANCEL", false,
			landNow, null, null, null);
}

function takeOff() {
	if (confirmationInProgress) {
		simpleMenuRequest("execute");
	} else
		askConfirmation("Are you sure you want to start the auto take-off sequence? " +
			"\nAfter takeoff, the drone will start flying over the waypoints",
			"TAKEOFF", "CANCEL", true,
			takeOff, null, null, null);
}

function fts() {
	if (confirmationInProgress) {
		simpleMenuRequest("fts");
	} else
		askConfirmation("Are you sure you want to terminate the flight?\nIF THE DRONE IS IN FLIGHT, IT WILL FALL!",
			"TERMINATE", "CANCEL", false,
			fts, null, null, null);
}

function simpleMenuRequest(request) {
	const xmlHTTP = new XMLHttpRequest();
	xmlHTTP.onreadystatechange = function () {
		if (this.readyState === 4) {
			const response = JSON.parse(this.responseText);
			if (this.status === 200 && response.status.toLowerCase() === "ok")
				closeMenu();
			else {
				if (response.status !== null && response.message !== null)
					cyberAlert("ERROR " + this.status, response.message);
				else
					cyberAlert("UNKNOWN ERROR", "");
			}
		}
	};

	/* Send execute API request */
	xmlHTTP.open("POST", "/api", true);
	xmlHTTP.setRequestHeader("Content-Type", "application/json");
	xmlHTTP.send(JSON.stringify({"action": request}));
}

function askConfirmation(text, confirmBtnText, cancelBtnText, swipeButtonColors,
						 ifConfirmed, confirmedArgument, ifCanceled, canceledArgument) {
	document.getElementById("dialog_text").innerText = text;
	document.getElementById("dialog_confirm").innerHTML = "<p>" + confirmBtnText + "</p>";
	document.getElementById("dialog_cancel").innerHTML = "<p>" + cancelBtnText + "</p>";
	if (swipeButtonColors) {
		document.getElementById("dialog_confirm").className = "button blue";
		document.getElementById("dialog_cancel").className = "button red";
	}
	else {
		document.getElementById("dialog_confirm").className = "button red";
		document.getElementById("dialog_cancel").className = "button blue";
	}
	actionIfConfirmed = ifConfirmed;
	actionArgumentIfConfirmed = confirmedArgument;
	actionIfCanceled = ifCanceled;
	actionArgumentIfCanceled = canceledArgument;
	confirmationInProgress = true;
	document.getElementById("overlay-container-dialog").style.display = "block";
	document.getElementById("confirmation_dialog").style.display = "block";
}

function dialogConfirm() {
	if (actionIfConfirmed !== null)
		actionIfConfirmed(actionArgumentIfConfirmed);
	confirmationInProgress = false;
	document.getElementById("overlay-container-dialog").style.display = "none";
	document.getElementById("confirmation_dialog").style.display = "none";
}

function dialogCancel() {
	if (actionIfCanceled !== null)
		actionIfCanceled(actionArgumentIfCanceled);
	confirmationInProgress = false;
	document.getElementById("overlay-container-dialog").style.display = "none";
	document.getElementById("confirmation_dialog").style.display = "none";
}

function cyberAlert(title, text) {
	document.getElementById("alert_title").innerText = title;
	document.getElementById("alert_text").innerText = text;
	document.getElementById("overlay-container-dialog").style.display = "block";
	document.getElementById("alert_dialog").style.display = "block";
}

function alertFinish() {
	document.getElementById("alert_dialog").style.display = "none";
	document.getElementById("overlay-container-dialog").style.display = "none";
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
					enableMapDisableCamera(true);
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
function shutDown() {
	if (confirmationInProgress || !takeoffDetected) {
		const xmlHTTP = new XMLHttpRequest();
		xmlHTTP.onreadystatechange = function() {
			if (this.readyState === 4 && this.status === 200
				&& JSON.parse(this.responseText).status.toLowerCase() === "ok")
				closeMenu();
				window.location.href = "/";
		};

		/* Send abort API request */
		xmlHTTP.open("POST", "/api", true);
		xmlHTTP.setRequestHeader("Content-Type", "application/json");
		xmlHTTP.send(JSON.stringify({ "action": "abort" }));

	} else
		askConfirmation("The drone is in flight! Are you sure you want to quit Liberty-Way? " +
			"THE DRONE WILL START THE AUTO LANDING SEQUENCE!",
			"LAND", "CANCEL", false,
			shutDown, null, null, null);


}