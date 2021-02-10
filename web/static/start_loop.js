function start_loop() {
	document.getElementById("btn_start").disabled = true;
	var camera_id = document.getElementById("camera_id").value;
	var camera_exp = document.getElementById("camera_exp").value;
	
	var start_location = "/start?camera_id=" + camera_id + "&camera_exp=" + camera_exp;
	
	if (document.getElementById("cnc_port_enabled").checked) {
		start_location += "&cnc_port=" + document.getElementById("cnc_ports").value;
		start_location += "&cnc_baudrate=" + document.getElementById("cnc_baudrate").value;
	}
		
	if (document.getElementById("rf_port_enabled").checked) {
		start_location += "&rf_port=" + document.getElementById("rf_ports").value;
		start_location += "&rf_baudrate=" + document.getElementById("rf_baudrate").value;
	}
		
	if (document.getElementById("udp_ip_port_enabled").checked)
		start_location += "&udp_ip_port=" + document.getElementById("udp_ip_port").value;
	
	window.location.href = start_location;
}