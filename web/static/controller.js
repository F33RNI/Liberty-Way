const bingAPIKey = "AnBO2H6OMskUam6VW9qK2-kD6dfzYFTH1hIXDsjRbsFfUbKX4SubZV8WnME0sntE";
let mapEnabled = false;

window.onload = onLoad;

const telemetryXMLHttp = new XMLHttpRequest();

let telemetryPackets = 0;
let telemetryLostFlag = true;

let map = null;
let homePin = null;
let dronePin = null;
let homePinSet = false;
let dronePinSet = false;
let backgroundStreamImg = null;

function onLoad() {
	checkVideoOrMap();

	telemetryXMLHttp.onreadystatechange = function () {
		if (this.readyState === 4) {
			if (this.status === 200) {
				const response = JSON.parse(this.responseText);
				if (response.status.toLowerCase() === "ok") {
					parseTelemetry(response.telemetry);
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

function initMap() {
	try {
		Microsoft.Maps.loadModule("Microsoft.Maps.Themes.BingTheme", {
			callback: function () {
				map = new Microsoft.Maps.Map(document.getElementById("map-container"),
					{
						credentials: bingAPIKey,
						enableClickableLogo: false,
						enableSearchLogo: false,
						center: new Microsoft.Maps.Location(0, 0),
						/* Microsoft.Maps.MapTypeId.road */
						mapTypeId: Microsoft.Maps.MapTypeId.aerial,
						zoom: 0,
						theme: new Microsoft.Maps.Themes.BingTheme()
					});
				mapEnabled = true;
			}
		});
	} catch (err) {
		mapEnabled = false;
		alert("Error loading map!\n" + err.message + "\nCheck your internet connection and reload the page");
	}
}

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
				initMap();
		} else
			initMap();
	};

	xmlHTTP.open("POST", "/api", true);
	xmlHTTP.setRequestHeader("Content-Type", "application/json");
	xmlHTTP.send(JSON.stringify({"action": "check_stream"}));
}

function disableMapEnableCamera() {
	mapEnabled = false;
	dronePinSet = false;
	homePinSet = false;

	document.getElementById("map-container").style.visibility = "hidden";

	/* Create background image */
	backgroundStreamImg = document.createElement('div');
	backgroundStreamImg.innerHTML = "<img class=\"background-video-stream-blurred\" src=\"/video_feed\" " +
		"alt=\"Platform camera\">" +
		"<img class=\"background-video-stream\" src=\"/video_feed\" alt=\"Platform camera\">";
	document.getElementById("stream-container").appendChild(backgroundStreamImg);
	document.getElementById("stream-container").style.visibility = "visible";
}

function enableMapDisableCamera() {
	initMap();
	document.getElementById("map-container").style.visibility = "visible";

	/* Delete image stream */
	backgroundStreamImg.remove();
	document.getElementById("stream-container").style.visibility = "hidden";
}

function requestTelemetry() {
	telemetryXMLHttp.open("POST", "/api", true);
	telemetryXMLHttp.setRequestHeader("Content-Type", "application/json");
	telemetryXMLHttp.send(JSON.stringify({"action": "telemetry"}));
}

function parseTelemetry(telemetry) {
	telemetryPackets++;

	/* Number of receiver packets */
	document.getElementById("telemetry-packets").innerHTML = telemetryPackets.toString();

	/* Timeline progress */
	document.getElementById("timeline-progress").style.width = telemetry.progress + "%";

	/* System status */
	document.getElementById("status").innerHTML = telemetry.status;

	/* Drone telemetry */
	document.getElementById("drone_packets").innerHTML = telemetry.drone_packets;
	document.getElementById("flight_mode").innerHTML = telemetry.flight_mode;
	document.getElementById("drone_voltage").innerHTML = telemetry.drone_voltage + " V";
	document.getElementById("drone_altitude").innerHTML = telemetry.drone_altitude + " m";
	document.getElementById("drone_satellites").innerHTML = telemetry.drone_satellites;
	document.getElementById("drone_lat").innerHTML = telemetry.drone_lat;
	document.getElementById("drone_lon").innerHTML = telemetry.drone_lon;

	/* Platform telemetry */
	document.getElementById("platform_packets").innerHTML = telemetry.platform_packets;
	document.getElementById("platform_speed").innerHTML = telemetry.platform_speed + " km/h";
	document.getElementById("platform_pressure").innerHTML = telemetry.platform_pressure + " Pa";
	document.getElementById("platform_satellites").innerHTML = telemetry.platform_satellites;
	document.getElementById("platform_lat").innerHTML = telemetry.platform_lat;
	document.getElementById("platform_lon").innerHTML = telemetry.platform_lon;

	putTelemetryOnMap(telemetry);
}

function putTelemetryOnMap(telemetry) {
	let loc;
	if (mapEnabled) {
		if (telemetry.drone_lat > 0.0 || telemetry.drone_lat < 0.0 ||
			telemetry.drone_lon > 0.0 || telemetry.drone_lon < 0.0) {
			loc = new Microsoft.Maps.Location(telemetry.drone_lat, telemetry.drone_lon);
			if (!dronePinSet) {
				dronePin = new Microsoft.Maps.Pushpin(loc, {text: 'D', color: 'red'});
				map.entities.push(dronePin);
				map.setView({center: loc, zoom: 20});
			}
			dronePin.setLocation(loc);
			dronePinSet = true;
		}

		if (telemetry.platform_lat > 0.0 || telemetry.platform_lat < 0.0 ||
			telemetry.platform_lon > 0.0 || telemetry.platform_lon < 0.0) {
			loc = new Microsoft.Maps.Location(telemetry.platform_lat, telemetry.platform_lon);
			if (!homePinSet) {
				homePin = new Microsoft.Maps.Pushpin(loc, {text: 'H', color: 'cyan'});
				map.entities.push(homePin);
			}
			homePin.setLocation(loc);
			homePinSet = true;
		}
	}
}

function telemetryRefresh(connected) {
	telemetryLostFlag = !connected;
	document.getElementById("telemetry-status").innerHTML = telemetryLostFlag ? "Lost" : "Connected";
	if (telemetryLostFlag) {
		console.log("Telemetry lost! Attempt to retry after 5 seconds");
		setTimeout(requestTelemetry, 5000);
	} else {
		setTimeout(requestTelemetry, 500);
	}
}

function execute() {
	const xmlHTTP = new XMLHttpRequest();
	xmlHTTP.onreadystatechange = function () {
		if (this.readyState === 4 && this.status === 200 && JSON.parse(this.responseText).status.toLowerCase() === "ok")
			document.getElementById("timeline-progress").style.width = "100%";
	};

	xmlHTTP.open("POST", "/api", true);
	xmlHTTP.setRequestHeader("Content-Type", "application/json");

	xmlHTTP.send(JSON.stringify({"action": "execute"}));
}

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

	xmlHTTP.open("POST", "/api", true);
	xmlHTTP.setRequestHeader("Content-Type", "application/json");

	xmlHTTP.send(JSON.stringify({"action": "toggle_stream"}));
}

function abort() {
	const xmlHTTP = new XMLHttpRequest();
	xmlHTTP.onreadystatechange = function() {
		if (this.readyState === 4 && this.status === 200 && JSON.parse(this.responseText).status.toLowerCase() === "ok")
			window.location.href = "/";
	};
	
	xmlHTTP.open("POST", "/api", true);
	xmlHTTP.setRequestHeader("Content-Type", "application/json");
	xmlHTTP.send(JSON.stringify({ "action": "abort" }));
}