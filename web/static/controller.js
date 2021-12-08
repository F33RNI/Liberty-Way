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
const mapboxStyle = "mapbox://styles/-fern-/cks4ssq6z54cw17mw53aym9ha";

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

// Liberty-Link steps
const LINK_STEP_TAKEOFF = 1;
const LINK_STEP_ASCENT = 2;
const LINK_STEP_WAYP_CALC = 3;
const LINK_STEP_GPS_WAYP = 4;
const LINK_STEP_GPS_SETP = 5;
const LINK_STEP_DESCENT = 6;
const LINK_STEP_SONARUS = 7;
const LINK_STEP_AFTER_SONARUS = 8;

// System variables
const telemetryXMLHttp = new XMLHttpRequest();
let telemetry = null;
let telemetryPackets = 0;
let map = null;
let homePin = null;
let isHomePinSet = false;
let isDronePinSet = false;
let backgroundStreamImg = null;
let waypointTempPin = null;
let takeoffDetected = false;
let confirmationInProgress = false;
let actionIfConfirmed = null;
let actionIfCanceled = null;
let actionArgumentIfConfirmed = null;
let actionArgumentIfCanceled = null;
let waypoints = [];
let waypointPins = [];
let addingFromMap = false;
let timelineTicker = false;
let timelineOldAutoLandingStep = -1;
let timelineOldWaypointIndex = -1;
let timelineOldWaypointAPI = -1;
let timelineOldLinkWaypointStep = -1;

// geoJSON for lines and drone point
const geoJsonLines = {
	"type": "FeatureCollection",
	"features": [
		{
			"type": "Feature",
			"geometry": {
				"type": "LineString",
				"coordinates": [[0, 0]]
			}
		}
	]
};
const geoJsonPoints = {
	"type": "FeatureCollection",
	"features": [
		{
			"type": "Feature",
			"geometry": {
				"type": "Point",
				"coordinates": []
			}
		}
	]
};

// Timeline strings
const timelinePlatform = ["Takeoff", "Ascending", "GPS Flight", "Descending", "Optical stab.", "Landing"];
const timelineFlyNoDescend = ["Takeoff", "Ascending", "GPS Flight", "Next waypoint"];
const timelineDescend = ["Takeoff", "Ascending", "GPS Flight", "Descending", "Waiting", "Next waypoint"];
const timelineParcel = ["Takeoff", "Ascending", "GPS Flight", "Descending", "Parcel drop", "Next waypoint"];
const timelineLand = ["Takeoff", "Ascending", "GPS Flight", "Descending", "Landing", "Motors off"];
const timelineAutoLanding = ["Landing"];

// Execute onLoad() function when the page is opened
window.onload = onLoad;


/**
 * Re-maps a number from one range to another. That is, a value of fromLow would get mapped to toLow,
 * a value of fromHigh to toHigh, values in-between to values in-between, etc
 * @param x the number to map
 * @param in_min the lower bound of the value’s current range
 * @param in_max the upper bound of the value’s current range
 * @param out_min the lower bound of the value’s target range
 * @param out_max the upper bound of the value’s target range
 * @returns {*} the mapped value
 */
function mapValue(x, in_min, in_max, out_min, out_max) {
	return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
}

/**
 * Initializes the map, telemetry reception and determines the view (camera / map) when the page is loaded
 */
function onLoad() {
	// Initialize the map
	try {
		// Set access token
		mapboxgl.accessToken = mapboxToken;

		// Create new map object
		map = new mapboxgl.Map({
			container: "map-container",
			style: mapboxStyle,
			zoom: -1
		});

		// Create platform marker
		homePin = new mapboxgl.Marker({
			color: "#99daff",
			draggable: false
		}).setPopup(new mapboxgl.Popup()
			.setText("Platform")).setLngLat([0, 0]);

		// Create temp marker (for new waypoint)
		waypointTempPin = new mapboxgl.Marker({
			draggable: true,
			color: "#54e34f"
		}).setPopup(new mapboxgl.Popup()
			.setText("New waypoint")).setLngLat([0, 0]);

		// Change text in map menu when dragging the waypoint
		function onDrag() {
			// Convert latitude to -90 - 90 range
			let lat = waypointTempPin.getLngLat().lat;
			if (lat < -90)
				lat += 180;
			else if (lat > 90)
				lat -= 180;

			// Convert longitude to -180 - 180 range
			let lon = waypointTempPin.getLngLat().lng;
			if (lon < -180)
				lon += 360;
			else if (lon > 180)
				lon -= 360;

			// Set latitude and longitude text
			document.getElementById("waypoint_map_lat").innerText = lat.toFixed(6).toString();
			document.getElementById("waypoint_map_lon").innerText = lon.toFixed(6).toString();
		}

		// Connect onDrag function
		waypointTempPin.on("drag", onDrag);

		// Add drone image and geoJSON data
		map.on("load", function () {
			// Add lines geoJson
			map.addSource("line", {
				"type": "geojson",
				"data": geoJsonLines
			});

			// Add drone point geoJson
			map.addSource("point", {
				"type": "geojson",
				"data": geoJsonPoints
			});

			// Add lines layer
			map.addLayer({
				"id": "lines",
				"type": "line",
				"source": "line",
				"paint": {
					"line-width": 3,
					"line-opacity": 0.8,
					"line-color": "#ed6498"
				}
			});

			// Load drone image
			map.loadImage(
				"drone.png",
				(error, image) => {
					if (error) throw error;
					map.addImage("drone", image);
				}
			);

			// Add drone point layer
			map.addLayer({
				"id": "points",
				"type": "symbol",
				"source": "point",
				"layout": {
					"icon-image": "drone",
					"icon-size": 0.2,
					"icon-allow-overlap" : true,
					"text-allow-overlap": true
				}
			});
		});

	}

	// Capturing an error while loading a map
	catch (err) {
		// Reset map
		map = null;

		// Show error alert
		cyberAlert("ERROR LOADING MAP",
			"Error message: " + err.message + "\nCheck your internet connection and reload the page");
	}

	// Select map or camera image as background
	checkVideoOrMap();

	// Cyclically sends request and receives telemetry
	telemetryXMLHttp.onreadystatechange = function () {
		if (this.readyState === 4) {
			if (this.status === 200) {
				// Parse response as JSON
				const response = JSON.parse(this.responseText);

				// If the response is successfully
				if (response.status.toLowerCase() === "ok") {

					// Store telemetry as global variable
					telemetry = response.telemetry;

					// Store takeoff detected status
					takeoffDetected = telemetry.takeoff_detected;

					// Set initial position for waypointTempPin
					if (waypointTempPin !== null && (waypointTempPin.getLngLat().lat === 0
						|| waypointTempPin.getLngLat().lon === 0)) {

						// Copy drone's position
						if (parseFloat(telemetry.drone_lat) !== 0 || parseFloat(telemetry.drone_lon) !== 0)
							waypointTempPin.setLngLat([parseFloat(telemetry.drone_lon),
								parseFloat(telemetry.drone_lat)]);

						// Copy platforms's position
						else if (parseFloat(telemetry.platform_lat) !== 0 || parseFloat(telemetry.platform_lon) !== 0)
							waypointTempPin.setLngLat([parseFloat(telemetry.platform_lon),
								parseFloat(telemetry.platform_lat)]);
					}

					// Waypoints update without menu
					waypointsUpdate(false);

					printTelemetry();
					telemetryRefresh(true);
				} else {
					console.log("Wrong response!\nServer response: " + this.responseText);
					telemetryRefresh(false);
				}
			}

			// Print error to the console and slow down telemetry refresh rate
			else {
				// Print response to the console
				console.log("Error sending POST request!\nHTTP Status: " + this.status
					+ "\nServer response: " + this.responseText);

				// Refresh telemetry with slow rate
				telemetryRefresh(false);
			}
		}
	};

	// Request first telemetry data
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

	// Send check_stream API request
	xmlHTTP.open("POST", "/api", true);
	xmlHTTP.setRequestHeader("Content-Type", "application/json");
	xmlHTTP.send(JSON.stringify({"action": "check_stream"}));
}

/**
 * Switches view to the camera
 */
function disableMapEnableCamera() {
	// Hide map container
	document.getElementById("map-container").style.visibility = "hidden";

	// Create background image
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
	// Show map container
	document.getElementById("map-container").style.visibility = "visible";

	// Delete image stream
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

	// Number of receiver packets
	document.getElementById("telemetry-packets").innerHTML = telemetryPackets.toString();

	// System status
	document.getElementById("status").innerHTML = telemetry.status;

	// Distance between drone and platform
	document.getElementById("distance").innerHTML = telemetry.distance + " m";

	// Drone telemetry
	document.getElementById("drone_packets").innerHTML = telemetry.drone_packets;
	document.getElementById("drone_voltage").innerHTML = telemetry.drone_voltage + " V";
	document.getElementById("drone_altitude").innerHTML = telemetry.drone_altitude + " m";
	document.getElementById("drone_satellites").innerHTML = telemetry.drone_satellites;
	document.getElementById("drone_lat").innerHTML = telemetry.drone_lat;
	document.getElementById("drone_lon").innerHTML = telemetry.drone_lon;
	document.getElementById("drone_speed").innerHTML = telemetry.drone_speed + " km/h";

	// Platform telemetry
	document.getElementById("platform_packets").innerHTML = telemetry.platform_packets;
	document.getElementById("platform_satellites").innerHTML = telemetry.platform_satellites;
	document.getElementById("platform_lat").innerHTML = telemetry.platform_lat;
	document.getElementById("platform_lon").innerHTML = telemetry.platform_lon;
	document.getElementById("platform_speed").innerHTML = telemetry.platform_speed + " km/h";
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
 * Requests array of waypoints
 * @param updateMenu set to true to update menu list of waypoints
 */
function waypointsUpdate(updateMenu) {
	const xmlHTTP = new XMLHttpRequest();
	xmlHTTP.onreadystatechange = function () {
		if (this.readyState === 4 && this.status === 200) {
			const response = JSON.parse(this.responseText);
			if (response.status.toLowerCase() === "ok") {

				// Store waypoints as global variable
				waypoints = response.waypoints;

				// Update waypoints on the map
				updateMap();

				// Update timeline container
				updateTimeline();

				// Menu waypoints list
				if (updateMenu) {
					// Remove previous items
					const scrollContainer = document.getElementById("scroll-container");
					const scrollElements = document.getElementsByClassName("scroll-element");
					while (scrollElements[0]) {
						scrollElements[0].parentNode.removeChild(scrollElements[0]);
					}

					// Set ID of new waypoint (in add new waypoint menu)
					document.getElementById("add_waypoint_text").innerText = "ADD WAYPOINT "
						+ (waypoints.length + 1);
					document.getElementById("edit_waypoint_text").innerText = "ADD WAYPOINT "
						+ (waypoints.length + 1);

					// Add waypoints if they exists
					if (waypoints.length > 0) {
						let waypointElement;
						for (let i = 0; i < waypoints.length; i++) {
							// Create new waypoint element
							waypointElement = document.createElement("div");
							waypointElement.className = "scroll-element";
							let labelsText = document.createElement("p");
							labelsText.className = "labels-text";

							// If waypoint is not skipped
							if (waypoints[i].api > WAYPOINT_SKIP) {
								// Add waypoint ID
								labelsText.innerHTML = "<a>" + (i + 1) + ": ";

								// Add GPS coordinates
								if (waypoints[i].api !== WAYPOINT_PLATFORM)
									labelsText.innerHTML += waypoints[i].lat + ", " + waypoints[i].lon;

								// Add action text
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

							// Show just skip message if waypoints[i].api <= WAYPOINT_SKIP
							else
								labelsText.innerHTML = "<a>" + (i + 1) + ": SKIP</a>";

							// Add delete button to all waypoints
							let deleteButton = document.createElement("div");
							deleteButton.className = "button red small";
							deleteButton.setAttribute("data-augmented-ui", "br-clip both");
							deleteButton.style.width = "4em";
							deleteButton.style.marginLeft = "1em";
							deleteButton.addEventListener("click",
								function () {
									deleteWaypoint(i);
								}, false);

							deleteButton.innerHTML = "<a>DELETE</a>";

							// Combine elements
							waypointElement.appendChild(labelsText);
							waypointElement.appendChild(deleteButton);
							scrollContainer.appendChild(waypointElement);
						}

						// Set buttons as the last item
						waypointElement.after(document.getElementById("scroll-buttons"));
					}
				}
			}
		}
	};

	// Send get_waypoints API request
	xmlHTTP.open("POST", "/api", true);
	xmlHTTP.setRequestHeader("Content-Type", "application/json");
	xmlHTTP.send(JSON.stringify({"action": "get_waypoints"}));
}

/**
 * Updates markers and lines on map
 */
function updateMap() {
	// Check if map is available
	if (map !== null) {
		try {
			// Clear lines array
			geoJsonLines.features[0].geometry.coordinates = [];

			// Clear waypoints array
			if (waypointPins.length > 0) {
				try {
					for (let i = 0; i < waypointPins.length; i++) {
						waypointPins[i].remove();
						waypointPins.splice(i, 1);
					}
				} catch (ignored) {
				}
			}

			// Check length of waypoints array
			if (waypoints.length > 0) {
				for (let i = 0; i < waypoints.length; i++) {
					// Don't add skipped waypoints on the map
					if (waypoints[i].api > WAYPOINT_SKIP) {
						// Add waypoints and lines
						if (waypoints[i].api !== WAYPOINT_PLATFORM) {
							// Create new marker
							let waypointPin = new mapboxgl.Marker({
								draggable: false,
								color: "#fafa64"
							}).setPopup(new mapboxgl.Popup()
								.setText("Waypoint " + (i + 1))).setLngLat([waypoints[i].lon, waypoints[i].lat])
								.addTo(map);

							// Add new marker to the array of markers
							waypointPins.push(waypointPin);

							// Draw lines between all waypoints
							geoJsonLines.features[0].geometry.coordinates.push([waypoints[i].lon, waypoints[i].lat]);
						}

						// Add line to the platform
						else if (telemetry !== null
							&& (telemetry.platform_lat !== "0" || telemetry.platform_lon !== "0")
							&& (parseFloat(telemetry.platform_lat) !== 0
								|| parseFloat(telemetry.platform_lon) !== 0))
							geoJsonLines.features[0].geometry.coordinates.push([parseFloat(telemetry.platform_lon),
								parseFloat(telemetry.platform_lat)]);
					}
				}
			}

			// Add empty point to the lines array
			else
				geoJsonLines.features[0].geometry.coordinates = [[0, 0]];

			// Update lines on the map
			map.getSource("line").setData(geoJsonLines);

			// Set platform marker
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

			// Clear platform marker
			else if (isHomePinSet) {
				homePin.remove();
				isHomePinSet = false;
			}

			// Set drone marker
			if (!telemetry.drone_telemetry_lost && telemetry.drone_satellites > 1) {
				geoJsonPoints.features[0].geometry.coordinates = [telemetry.drone_lon, telemetry.drone_lat];
				map.getSource("point").setData(geoJsonPoints);
				if (!isDronePinSet) {
					map.setZoom(initialZoomLevel);
					map.panTo([telemetry.drone_lon, telemetry.drone_lat]);
					isDronePinSet = true;
				}
			}

			// Clear drone marker
			else if (isDronePinSet) {
				geoJsonPoints.features[0].geometry.coordinates = [];
				map.getSource("point").setData(geoJsonPoints);
				isDronePinSet = false;
			}
		}

		// Print error message to the console
		catch (e) {
			console.error("Error updating map", e);
		}
	}
}

/**
 * Updates timeline container
 */
function updateTimeline() {
	try {
		// Get document elements
		tickmarksTop = document.getElementById("tickmarks_top");
		tickmarksTopMarks = document.getElementById("tickmarks_top_marks");
		tickmarksBottom = document.getElementById("tickmarks_bottom");
		timelineWaypointText = document.getElementById("timeline_waypoint_text");

		// Update timeline only if telemetry is not null
		if (telemetry != null) {
			// Update timeline only if waypoints.length > 0
			if (waypoints.length > 0) {
				// Update timeline if anything has changed
				if (timelineOldAutoLandingStep !== telemetry.auto_landing_step
					|| timelineOldWaypointIndex !== telemetry.waypoint_index
					|| timelineOldWaypointAPI !== waypoints[telemetry.waypoint_index].api
					|| timelineOldLinkWaypointStep !== telemetry.link_waypoint_step) {
					// Clear all tickmarks
					tickmarksTop.innerHTML = "";
					tickmarksTopMarks.innerHTML = "";
					tickmarksBottom.innerHTML = "";

					// Select right layout
					let tickmarsLayout = [""];
					if (telemetry.auto_landing_step === 0) {
						switch (waypoints[telemetry.waypoint_index].api) {
							case WAYPOINT_PLATFORM:
								tickmarsLayout = timelinePlatform;
								break;
							case WAYPOINT_FLY:
								tickmarsLayout = timelineFlyNoDescend;
								break;
							case WAYPOINT_DESCENT:
								tickmarsLayout = timelineDescend;
								break;
							case WAYPOINT_PARCEL:
								tickmarsLayout = timelineParcel;
								break;
							case WAYPOINT_LAND:
								tickmarsLayout = timelineLand;
								break;
							default:
								break;
						}
					}

					// Auto-landing layout
					else
						tickmarsLayout = timelineAutoLanding;

					for (let i = 0; i < tickmarsLayout.length; i++) {
						// Add even tickmarks
						if (i % 2 === 0) {
							tickmarksTop.innerHTML += "<p style=\"width: 0;\">"
								+ tickmarsLayout[i].replaceAll(" ", "&nbsp;") + "</p>";
							tickmarksTopMarks.innerHTML += "<p></p>";
							tickmarksBottom.innerHTML += "<p style=\"line-height: 2.5em; width: 0;\"></p>";
						}

						// Add odd tickmarks
						else {
							tickmarksTop.innerHTML += "<p style=\"width: 0;\"></p>";
							tickmarksTopMarks.innerHTML += "<p style=\"width: 0;\"></p>";
							tickmarksBottom.innerHTML += "<p style=\"line-height: 2.5em;\">"
								+ tickmarsLayout[i].replaceAll(" ", "&nbsp;") + "</p>";
						}
					}

					// Waypoint mode
					if (telemetry.auto_landing_step === 0) {
						// Beginning of the text
						timelineWaypointText.innerHTML = "WAYPOINT " + (telemetry.waypoint_index + 1) + ": ";

						if (waypoints[telemetry.waypoint_index].api > WAYPOINT_SKIP) {
							if (waypoints[telemetry.waypoint_index].api !== WAYPOINT_PLATFORM) {
								// Add GPS coordinates of the waypoint
								timelineWaypointText.innerHTML += waypoints[telemetry.waypoint_index].lat + ", "
									+ waypoints[telemetry.waypoint_index].lon + " ";

								// Add waypoint mode
								switch (waypoints[telemetry.waypoint_index].api) {
									case WAYPOINT_FLY:
										timelineWaypointText.innerHTML += "JUST FLY";
										break;
									case WAYPOINT_DESCENT:
										timelineWaypointText.innerHTML += "DESCENT";
										break;
									case WAYPOINT_PARCEL:
										timelineWaypointText.innerHTML += "PARCEL";
										break;
									case WAYPOINT_LAND:
										timelineWaypointText.innerHTML += "LAND HERE";
										break;
									default:
										break;
								}
							}

							// Add platform GPS coordinates and text
							else if ((telemetry.platform_lat !== "0" || telemetry.platform_lon !== "0")
								&& (parseFloat(telemetry.platform_lat) !== 0
									|| parseFloat(telemetry.platform_lon) !== 0)) {
								timelineWaypointText.innerHTML +=
									telemetry.platform_lat + ", " + telemetry.platform_lon + " PLATFORM";
							}
						} else
							timelineWaypointText.innerHTML += "SKIP";
					}

					// Auto-landing mode
					else
						timelineWaypointText.innerHTML = "AUTO-LANDING";

					// Add last empty tickmark
					tickmarksTop.innerHTML += "<p style=\"width: 0;\"></p>";
					tickmarksTopMarks.innerHTML += "<p style=\"width: 0;\"></p>";
					tickmarksBottom.innerHTML += "<p style=\"line-height: 2.5em; width: 0;\"></p>";

					// Store previous values
					timelineOldAutoLandingStep = telemetry.auto_landing_step;
					timelineOldWaypointAPI = waypoints[telemetry.waypoint_index].api;
					timelineOldWaypointIndex = telemetry.waypoint_index;
					timelineOldLinkWaypointStep = telemetry.link_waypoint_step;
				}
			} else
				timelineWaypointText.innerHTML = "NO WAYPOINTS";
		} else
			timelineWaypointText.innerHTML = "NO TELEMETRY";

		// Update timeline progress
		timelineSetProgress();
	}

	// Print error message to the console
	catch (e) {
		console.error("Error updating timeline", e);
	}
}

/**
 * Sets width of timeline-progress element based on waypoints and telemetry
 */
function timelineSetProgress() {
	// Get timeline progress element
	let timelineProgress = document.getElementById("timeline-progress");

	// Show progress only if drone is in flight
	if (takeoffDetected) {
		// Waypoints mode
		if (telemetry.auto_landing_step === 0) {
			switch (waypoints[telemetry.waypoint_index].api) {
				case WAYPOINT_PLATFORM:
					// Platform
					if (telemetry.status !== "WAYP") {
						switch (telemetry.status) {
							case "LAND":
								timelineSetMappedProgress(timelinePlatform.length,
									mapValue(telemetry.sonarus_distance_cm, 510., 0., 5., 6.));
								break;
							default:
								timelineSetMappedProgress(timelinePlatform.length, 5);
								break;
						}
					} else
						switch (telemetry.link_waypoint_step) {
							case LINK_STEP_TAKEOFF:
								timelineSetMappedProgress(timelinePlatform.length, 1);
								break;
							case LINK_STEP_ASCENT:
								timelineSetMappedProgress(timelinePlatform.length, 2);
								break;
							case LINK_STEP_WAYP_CALC:
							case LINK_STEP_GPS_WAYP:
							case LINK_STEP_GPS_SETP:
								timelineSetMappedProgress(timelinePlatform.length, 3);
								break;
							default:
								timelineSetMappedProgress(timelinePlatform.length, 4);
								break;
						}
					break;

				case WAYPOINT_FLY:
					// Just fly
					switch (telemetry.link_waypoint_step) {
						case LINK_STEP_TAKEOFF:
							timelineSetMappedProgress(timelineFlyNoDescend.length, 1);
							break;
						case LINK_STEP_ASCENT:
							timelineSetMappedProgress(timelineFlyNoDescend.length, 2);
							break;
						case LINK_STEP_WAYP_CALC:
						case LINK_STEP_GPS_WAYP:
						case LINK_STEP_GPS_SETP:
							timelineSetMappedProgress(timelineFlyNoDescend.length, 3);
							break;
						default:
							timelineSetMappedProgress(timelineFlyNoDescend.length, 4);
							break;
					}
					break;

				case WAYPOINT_DESCENT:
					// Descending
					switch (telemetry.link_waypoint_step) {
						case LINK_STEP_TAKEOFF:
							timelineSetMappedProgress(timelineDescend.length, 1);
							break;
						case LINK_STEP_ASCENT:
							timelineSetMappedProgress(timelineDescend.length, 2);
							break;
						case LINK_STEP_WAYP_CALC:
						case LINK_STEP_GPS_WAYP:
						case LINK_STEP_GPS_SETP:
							timelineSetMappedProgress(timelineDescend.length, 3);
							break;
						case LINK_STEP_DESCENT:
						case LINK_STEP_SONARUS:
							timelineSetMappedProgress(timelineDescend.length, 4);
							break;
						case LINK_STEP_AFTER_SONARUS:
							timelineSetMappedProgress(timelineDescend.length, 5);
							break;
						default:
							timelineSetMappedProgress(timelineDescend.length, 6);
							break;
					}
					break;

				case WAYPOINT_PARCEL:
					// Parcel
					switch (telemetry.link_waypoint_step) {
						case LINK_STEP_TAKEOFF:
							timelineSetMappedProgress(timelineParcel.length, 1);
							break;
						case LINK_STEP_ASCENT:
							timelineSetMappedProgress(timelineParcel.length, 2);
							break;
						case LINK_STEP_WAYP_CALC:
						case LINK_STEP_GPS_WAYP:
						case LINK_STEP_GPS_SETP:
							timelineSetMappedProgress(timelineParcel.length, 3);
							break;
						case LINK_STEP_DESCENT:
						case LINK_STEP_SONARUS:
							timelineSetMappedProgress(timelineParcel.length, 4);
							break;
						case LINK_STEP_AFTER_SONARUS:
							timelineSetMappedProgress(timelineParcel.length, 5);
							break;
						default:
							timelineSetMappedProgress(timelineParcel.length, 6);
							break;
					}
					break;

				case WAYPOINT_LAND:
					// Land
					switch (telemetry.link_waypoint_step) {
						case LINK_STEP_TAKEOFF:
							timelineSetMappedProgress(timelineLand.length, 1);
							break;
						case LINK_STEP_ASCENT:
							timelineSetMappedProgress(timelineLand.length, 2);
							break;
						case LINK_STEP_WAYP_CALC:
						case LINK_STEP_GPS_WAYP:
						case LINK_STEP_GPS_SETP:
							timelineSetMappedProgress(timelineLand.length, 3);
							break;
						case LINK_STEP_DESCENT:
						case LINK_STEP_SONARUS:
						case LINK_STEP_AFTER_SONARUS:
							timelineSetMappedProgress(timelineLand.length, 4);
							break;
						default:
							timelineSetMappedProgress(timelineLand.length, 4);
							break;
					}
					break;

				default:
					// Skip
					timelineProgress.style.width = "0";
					break;
			}
		}

		// Auto-landing mode
		else
			timelineProgress.style.width =
				mapValue(telemetry.sonarus_distance_cm, 510, 0, 0, 100).toString() + "%";
	} else
		timelineProgress.style.width = "0";
}

/**
 * Draws progress steps on the timeline
 * @param numOfIntervals number of intervals into which the timeline is divided
 * @param fillInterval how many of these steps need to be painted
 */
function timelineSetMappedProgress(numOfIntervals, fillInterval) {
	// Calculate number of percents per one step
	const percentsPerStep = 100 / numOfIntervals;

	// Blinking timeline function
	if (fillInterval > 0 && timelineTicker)
		fillInterval -= 1;
	timelineTicker = !timelineTicker;

	// Set calculated width
	document.getElementById("timeline-progress").style.width = (fillInterval * percentsPerStep) + "%";
}

/**
 * Opens waypoints menu
 */
function menu() {
	// Update waypoints with menu list
	waypointsUpdate(true);

	// Clear selections
	document.getElementById("manual_action").selectedIndex = null;
	document.getElementById("add_waypoint").selectedIndex = null;

	// Disable other blocks
	document.getElementById("from_map_menu").style.display = "none";
	document.getElementById("set_manually_menu").style.display = "none";
	document.getElementById("add_waypoint_menu").style.display = "none";

	// Enable menu and overlay blocks
	document.getElementById("menu").style.display = "block";
	document.getElementById("overlay-container").style.display = "block";
}

/**
 * Opens add_waypoint menu
 */
function createWaypoint() {
	// Hide main menu and open add_waypoint menu
	document.getElementById("menu").style.display = "none";
	document.getElementById("add_waypoint_menu").style.display = "block";

	// Clear selections
	document.getElementById("manual_action").selectedIndex = null;
	document.getElementById("add_waypoint").selectedIndex = null;
}

/**
 * Sends delete_waypoint request by id
 * @param id integer id of selected waypoint to be deleted
 */
function deleteWaypoint(id) {
	// Request a confirmation
	if (confirmationInProgress) {
		// Refresh menu on result
		const xmlHTTP = new XMLHttpRequest();
		xmlHTTP.onreadystatechange = function () {
			if (this.readyState === 4)
				menu();
		};

		// Send delete_waypoint API request
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

/**
 * Opens a submenu when adding a waypoint
 * @param option manual/map/drone/platform
 */
function addWaypoint(option) {
	// Close add waypoint menu
	document.getElementById("add_waypoint_note").innerText = "";
	document.getElementById("apply-platform").style.display = "none";

	switch (option) {
		case ("manual"):
			// Open add manual menu
			document.getElementById("add_waypoint_menu").style.display = "none";
			document.getElementById("set_manually_menu").style.display = "block";
			document.getElementById("manual_action").selectedIndex = null;
			break;

		case ("map"):
			// Open add from map menu
			addingFromMap = true;

			// Move point a little
			waypointTempPin.setLngLat([waypointTempPin.getLngLat().lng + 0.00005,
				waypointTempPin.getLngLat().lat + 0.00005]);

			document.getElementById("add_waypoint_menu").style.display = "none";
			enableMapDisableCamera(false);
			document.getElementById("map-container").style.zIndex = "70";
			document.getElementById("from_map_menu").style.display = "block";
			waypointTempPin.addTo(map);
			break;

		case ("drone"):
			// Open add manual menu with drones position
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
			// Enable apply button to add platform waypoint
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

/**
 * Adds platform waypoint
 */
function applyPlatform() {
	// Refresh menu on result
	const xmlHTTP = new XMLHttpRequest();
	xmlHTTP.onreadystatechange = function () {
		if (this.readyState === 4)
			menu();
	};

	// Send add_waypoint API request
	xmlHTTP.open("POST", "/api", true);
	xmlHTTP.setRequestHeader("Content-Type", "application/json");
	xmlHTTP.send(JSON.stringify({"action": "add_waypoint", "api" : WAYPOINT_PLATFORM,
		"lat" : telemetry.platform_lat.toString(),
		"lon" : telemetry.platform_lon.toString()}));
}

/**
 * Adds new waypoint
 */
function applyManual() {
	// Refresh menu on result
	const xmlHTTP = new XMLHttpRequest();
	xmlHTTP.onreadystatechange = function () {
		if (this.readyState === 4)
			menu();
	};

	// Convert selected action to API's integer value
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

	// Send add_waypoint API request
	xmlHTTP.open("POST", "/api", true);
	xmlHTTP.setRequestHeader("Content-Type", "application/json");
	xmlHTTP.send(JSON.stringify({"action": "add_waypoint", "api" : manualAPI,
		"lat" : document.getElementById("manual_lat").value,
		"lon" : document.getElementById("manual_lon").value}));
}

/**
 * Writes the coordinates of the selected point on the map
 */
function applyFromMap() {
	// Convert latitude to -90 - 90 range
	let lat = waypointTempPin.getLngLat().lat;
	if (lat < -90)
		lat += 180;
	else if (lat > 90)
		lat -= 180;

	// Convert longitude to -180 - 180 range
	let lon = waypointTempPin.getLngLat().lng;
	if (lon < -180)
		lon += 360;
	else if (lon > 180)
		lon -= 360;

	// Write latitude and longitude to the manual menu
	document.getElementById("manual_lat").value = lat.toFixed(6);
	document.getElementById("manual_lon").value = lon.toFixed(6);

	// Hide map
	closeFromMap();

	// Open manual menu
	addWaypoint("manual");

	// Clear addingFromMap flag
	addingFromMap = false;
}

/**
 * Clears selections and hides selection from map
 */
function closeFromMap() {
	// Clear selections
	document.getElementById("manual_action").selectedIndex = null;
	document.getElementById("add_waypoint").selectedIndex = null;

	// Hide from map overlay
	document.getElementById("from_map_menu").style.display = "none";

	// Remove waypointTempPin from map
	waypointTempPin.remove();

	// Hide map and show add waypoint menu
	document.getElementById("map-container").style.zIndex = "0";
	document.getElementById("add_waypoint_menu").style.display = "block";

	// Switch to previous mode (map or cameras)
	checkVideoOrMap();

	// Clear addingFromMap flag
	addingFromMap = false;
}

/**
 * Clears selections and hides add set manually menu
 */
function closeManual() {
	// Clear selections
	document.getElementById("manual_action").selectedIndex = null;
	document.getElementById("add_waypoint").selectedIndex = null;

	// Hide add manually menu and show add waypoint menu
	document.getElementById("set_manually_menu").style.display = "none";
	document.getElementById("add_waypoint_menu").style.display = "block";
}

/**
 * Clears selections and hides menu
 */
function closeMenu() {
	// Clear selections
	document.getElementById("manual_action").selectedIndex = null;
	document.getElementById("add_waypoint").selectedIndex = null;

	// Disable all blocks
	document.getElementById("from_map_menu").style.display = "none";
	document.getElementById("set_manually_menu").style.display = "none";
	document.getElementById("add_waypoint_menu").style.display = "none";
	document.getElementById("menu").style.display = "none";
	document.getElementById("overlay-container").style.display = "none";
}

/**
 * Sends auto-land API request after confirmation
 */
function landNow() {
	// Check is the drone is in flight
	if (takeoffDetected) {
		if (confirmationInProgress) {
			simpleMenuRequest("land");
		} else
			askConfirmation("Are you sure you want to start the auto-landing sequence now? " +
				"\nThe current flight will be canceled and the drone will begin descending",
				"LAND", "CANCEL", false,
				landNow, null, null, null);
	}

	// Show error message
	else
		cyberAlert("UNABLE TO LAND", "The drone is not in flight. The operation was canceled!");
}

/**
 * Sends takeoff API request after confirmation
 */
function takeOff() {
	// Check is the drone is in flight
	if (!takeoffDetected) {
		if (confirmationInProgress) {
			simpleMenuRequest("execute");
		} else
			askConfirmation("Are you sure you want to start the auto take-off sequence? " +
				"\nAfter takeoff, the drone will start flying over the waypoints",
				"TAKEOFF", "CANCEL", true,
				takeOff, null, null, null);
	}

	// Show error message
	else
		cyberAlert("UNABLE TO TAKEOFF", "The drone is already in flight. The operation was canceled!");
}

/**
 * Sends FTS API request after confirmation
 */
function fts() {
	if (confirmationInProgress) {
		simpleMenuRequest("fts");
	} else
		askConfirmation("Are you sure you want to terminate the flight?\nIF THE DRONE IS IN FLIGHT, IT WILL FALL!",
			"TERMINATE", "CANCEL", false,
			fts, null, null, null);
}

/**
 * Creates simple API request and closes menu on response
 * @param request request String (of action)
 */
function simpleMenuRequest(request) {
	const xmlHTTP = new XMLHttpRequest();
	xmlHTTP.onreadystatechange = function () {
		if (this.readyState === 4) {
			const response = JSON.parse(this.responseText);
			// Close menu if response is OK
			if (this.status === 200 && response.status.toLowerCase() === "ok")
				closeMenu();

			// Show error alert
			else {
				if (response.status !== null && response.message !== null)
					cyberAlert("ERROR " + this.status, response.message);
				else
					cyberAlert("UNKNOWN ERROR", "");
			}
		}
	};

	// Send execute API request
	xmlHTTP.open("POST", "/api", true);
	xmlHTTP.setRequestHeader("Content-Type", "application/json");
	xmlHTTP.send(JSON.stringify({"action": request}));
}

/**
 * Creates confirmation dialog
 * @param text String text
 * @param confirmBtnText text on left button
 * @param cancelBtnText text on right button
 * @param swipeButtonColors set to true to make left (confirm) button blue and right (cancel) button red
 * @param ifConfirmed function to call if confirmed
 * @param confirmedArgument argument of ifConfirmed function
 * @param ifCanceled function to call if canceled
 * @param canceledArgument argument of ifCanceled function
 */
function askConfirmation(text, confirmBtnText, cancelBtnText, swipeButtonColors,
						 ifConfirmed, confirmedArgument, ifCanceled, canceledArgument) {
	// Set message and buttons text
	document.getElementById("dialog_text").innerText = text;
	document.getElementById("dialog_confirm").innerHTML = "<p>" + confirmBtnText + "</p>";
	document.getElementById("dialog_cancel").innerHTML = "<p>" + cancelBtnText + "</p>";

	// Set buttons color
	if (swipeButtonColors) {
		document.getElementById("dialog_confirm").className = "button blue";
		document.getElementById("dialog_cancel").className = "button red";
	}
	else {
		document.getElementById("dialog_confirm").className = "button red";
		document.getElementById("dialog_cancel").className = "button blue";
	}

	// Store functions
	actionIfConfirmed = ifConfirmed;
	actionArgumentIfConfirmed = confirmedArgument;
	actionIfCanceled = ifCanceled;
	actionArgumentIfCanceled = canceledArgument;

	// Set confirmationInProgress flag
	confirmationInProgress = true;

	// Show the confirmation dialog
	document.getElementById("overlay-container-dialog").style.display = "block";
	document.getElementById("confirmation_dialog").style.display = "block";
}

/**
 * Executes actionIfConfirmed with actionArgumentIfConfirmed and hides confirmation dialog
 */
function dialogConfirm() {
	// Execute actionIfConfirmed with actionArgumentIfConfirmed argument
	if (actionIfConfirmed !== null)
		actionIfConfirmed(actionArgumentIfConfirmed);

	// Clear confirmationInProgress flag
	confirmationInProgress = false;

	// Hide the confirmation dialog
	document.getElementById("overlay-container-dialog").style.display = "none";
	document.getElementById("confirmation_dialog").style.display = "none";
}

/**
 * Executes actionIfCanceled with actionArgumentIfCanceled and hides confirmation dialog
 */
function dialogCancel() {
	// Execute actionIfCanceled with actionArgumentIfCanceled argument
	if (actionIfCanceled !== null)
		actionIfCanceled(actionArgumentIfCanceled);

	// Clear confirmationInProgress flag
	confirmationInProgress = false;

	// Hide the confirmation dialog
	document.getElementById("overlay-container-dialog").style.display = "none";
	document.getElementById("confirmation_dialog").style.display = "none";
}

/**
 * Creates alert with title, text and OK button
 * @param title String title
 * @param text String text
 */
function cyberAlert(title, text) {
	document.getElementById("alert_title").innerText = title;
	document.getElementById("alert_text").innerText = text;
	document.getElementById("overlay-container-dialog").style.display = "block";
	document.getElementById("alert_dialog").style.display = "block";
}

/**
 * Hides the alert
 */
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

	// Send toggle_stream API request
	xmlHTTP.open("POST", "/api", true);
	xmlHTTP.setRequestHeader("Content-Type", "application/json");
	xmlHTTP.send(JSON.stringify({"action": "toggle_stream"}));
}

/**
 * Exits the application
 */
function shutDown() {
	// Action confirmed
	if (confirmationInProgress) {
		const xmlHTTP = new XMLHttpRequest();
		xmlHTTP.onreadystatechange = function() {
			if (this.readyState === 4 && this.status === 200
				&& JSON.parse(this.responseText).status.toLowerCase() === "ok")
				closeMenu();
			window.location.href = "/";
		};

		// Send abort API request
		xmlHTTP.open("POST", "/api", true);
		xmlHTTP.setRequestHeader("Content-Type", "application/json");
		xmlHTTP.send(JSON.stringify({ "action": "abort" }));
	}

	// Ask for confirmation
	else {
		if (!takeoffDetected)
			askConfirmation("Are you sure you want to shut down Liberty-Way?",
				"SHUT DOWN", "CANCEL", false,
				shutDown, null, null, null);
		else
			askConfirmation("The drone is in flight! Are you sure you want to shut down Liberty-Way? " +
				"THE DRONE WILL START THE AUTO LANDING SEQUENCE!",
				"LAND", "CANCEL", false,
				shutDown, null, null, null);
	}
}
