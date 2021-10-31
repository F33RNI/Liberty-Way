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

const xmlHTTP = new XMLHttpRequest();
let startBtnPressed = false;

window.onload = checkSerialPorts;

function checkSerialPorts() {
	/* Select link and platform udp checkboxes if more then 0 ports available */
	if (document.getElementById("link_ports").length > 0) {
		document.getElementById("link_port_enabled").checked = true;
		document.getElementById("platform_udp_enabled").checked = true;
	}

	/* Select UDP checkboxes if 0 ports available */
	if (document.getElementById("link_ports").length === 0 &&
		document.getElementById("platform_ports").length === 0) {
		document.getElementById("link_udp_enabled").checked = true;
		document.getElementById("platform_udp_enabled").checked = true;
	}
}

function checkboxSelectorPlatformCOM() {
	if (document.getElementById("platform_port_enabled").checked)
		document.getElementById("platform_udp_enabled").checked = false;
}

function checkboxSelectorLinkCOM() {
	if (document.getElementById("link_port_enabled").checked)
		document.getElementById("link_udp_enabled").checked = false;
}

function checkboxSelectorPlatformUDP() {
	if (document.getElementById("platform_udp_enabled").checked)
		document.getElementById("platform_port_enabled").checked = false;
}

function checkboxSelectorLinkUDP() {
	if (document.getElementById("link_udp_enabled").checked)
		document.getElementById("link_port_enabled").checked = false;
}

function startController() {
	if (!startBtnPressed) {
		const jsonRequest = {};

		jsonRequest.action = "setup";
		jsonRequest.camera_id = document.getElementById("camera_id").value;

		if (document.getElementById("link_port_enabled").checked) {
			jsonRequest.link_port = document.getElementById("link_ports").value;
			jsonRequest.link_baudrate = document.getElementById("link_baudrate").value;
		} else {
			jsonRequest.link_port = "";
		}

		if (document.getElementById("link_udp_enabled").checked) {
			jsonRequest.link_udp = document.getElementById("link_udp").value;
			jsonRequest.link_udp_tx = document.getElementById("link_udp_tx").value;
		} else {
			jsonRequest.link_udp = "";
			jsonRequest.link_udp_tx = "";
		}

		if (document.getElementById("platform_port_enabled").checked) {
			jsonRequest.platform_port = document.getElementById("platform_ports").value;
			jsonRequest.platform_baudrate = document.getElementById("platform_baudrate").value;
		} else {
			jsonRequest.platform_port = "";
		}

		if (document.getElementById("platform_udp_enabled").checked) {
			jsonRequest.platform_udp = document.getElementById("platform_udp").value;
			jsonRequest.platform_udp_tx = document.getElementById("platform_udp_tx").value;
		} else {
			jsonRequest.platform_udp = "";
			jsonRequest.platform_udp_tx = "";
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