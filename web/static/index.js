const xmlHTTP = new XMLHttpRequest();
let startBtnPressed = false;

window.onload = checkSerialPorts;

function checkSerialPorts() {
	/* Select link checkbox if more then 0 ports available */
	if (document.getElementById("link_ports").length > 0)
		document.getElementById("link_port_enabled").checked = true;

	/* Don't select platform checkbox if only one port provided for the both selectors */
	if (document.getElementById("platform_ports").length > 0
		&& !(document.getElementById("platform_ports").length === 1
			&& document.getElementById("link_ports").length === 1 &&
			document.getElementById("platform_ports").value ===
			document.getElementById("link_ports").value))
		document.getElementById("platform_port_enabled").checked = true;

	/* Select UDP checkbox if 0 ports available */
	if (document.getElementById("link_ports").length === 0 &&
		document.getElementById("platform_ports").length === 0)
		document.getElementById("udp_ip_port_enabled").checked = true;
}

function startController() {
	if (!startBtnPressed) {
		const jsonRequest = {};

		jsonRequest.action = "setup";
		jsonRequest.camera_id = document.getElementById("camera_id").value;

		if (document.getElementById("platform_port_enabled").checked) {
			jsonRequest.platform_port = document.getElementById("platform_ports").value;
			jsonRequest.platform_baudrate = document.getElementById("platform_baudrate").value;
		} else {
			jsonRequest.platform_port = "";
		}

		if (document.getElementById("link_port_enabled").checked) {
			jsonRequest.link_port = document.getElementById("link_ports").value;
			jsonRequest.link_baudrate = document.getElementById("link_baudrate").value;
		} else {
			jsonRequest.link_port = "";
		}

		if (document.getElementById("udp_ip_port_enabled").checked) {
			jsonRequest.udp_ip_port = document.getElementById("udp_ip_port").value;
		} else {
			jsonRequest.udp_ip_port = "";
		}

		xmlHTTP.onreadystatechange = function () {
			if (this.readyState === 4) {
				if (this.status === 200) {
					const response = JSON.parse(this.responseText);
					if (response.status.toLowerCase() === "ok") {
						window.location.href = "/";
					} else {
						alert("Wrong response!\nServer response: " + this.responseText);
						startBtnPressed = false;
					}
				} else {
					alert("Error sending POST request!\nHTTP Status: " + this.status
						+ "\nServer response: " + this.responseText);
					startBtnPressed = false;
				}
			}
		};

		xmlHTTP.open("POST", "/api", true);
		xmlHTTP.setRequestHeader("Content-Type", "application/json");

		xmlHTTP.send(JSON.stringify(jsonRequest));

		startBtnPressed = true;
	}
}

function abort() {
	xmlHTTP.onreadystatechange = function () {
		if (this.readyState === 4) {
			if (this.status === 200) {
				const response = JSON.parse(this.responseText);
				if (response.status.toLowerCase() === "ok") {
					window.location.href = "/";
				} else {
					alert("Wrong response!\nServer response: " + this.responseText);
				}

			} else {
				alert("Error sending POST request!\nHTTP Status: " + this.status
					+ "\nServer response: " + this.responseText);
			}
		}
	};

	xmlHTTP.open("POST", "/api", true);
	xmlHTTP.setRequestHeader("Content-Type", "application/json");

	xmlHTTP.send(JSON.stringify({"action": "abort"}));
}