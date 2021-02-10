function submit_values() {
	var pid_roll_p = parseFloat(document.getElementById('pid_roll_p').value);
	var pid_roll_i = parseFloat(document.getElementById('pid_roll_i').value);
	var pid_roll_d = parseFloat(document.getElementById('pid_roll_d').value);
	var pid_alt_p = parseFloat(document.getElementById('pid_alt_p').value);
	var pid_alt_i = parseFloat(document.getElementById('pid_alt_i').value);
	var pid_alt_d = parseFloat(document.getElementById('pid_alt_d').value);
	var video_enabled = document.getElementById('video_enabled').checked;
	window.location.href = '/setup?pid_roll_p=' + pid_roll_p + '&pid_roll_i=' + pid_roll_i + '&pid_roll_d=' + pid_roll_d + '&pid_alt_p=' + pid_alt_p + '&pid_alt_i=' + pid_alt_i + '&pid_alt_d=' + pid_alt_d + '&video=' + video_enabled;
}
function holding_changed(cb) {
	if (!cb.checked)
		document.getElementById("landing_drone").checked = false;
	window.location.href = '/hold_land?hold=' + cb.checked + '&land=' + document.getElementById("landing_drone").checked;
}
function landing_changed(cb) {
	if (cb.checked && !document.getElementById("holding_drone").checked)
		document.getElementById("landing_drone").checked = false;
	else
		window.location.href = '/hold_land/?hold=' + document.getElementById("holding_drone").checked + '&land=' + cb.checked;
}
function increase_p(element_id) {
	var new_value = parseFloat(document.getElementById(element_id).value) + 0.1;
	if (new_value >= 0)
		document.getElementById(element_id).value = new_value.toFixed(2).toString();
}
function decrease_p(element_id) {
	var new_value = parseFloat(document.getElementById(element_id).value) - 0.1;
	if (new_value >= 0)
		document.getElementById(element_id).value = new_value.toFixed(2).toString();
}
function increase_i(element_id) {
	var new_value = parseFloat(document.getElementById(element_id).value) + 0.002;
	if (new_value >= 0)
		document.getElementById(element_id).value = new_value.toFixed(3).toString();
}
function decrease_i(element_id) {
	var new_value = parseFloat(document.getElementById(element_id).value) - 0.002;
	if (new_value >= 0)
		document.getElementById(element_id).value = new_value.toFixed(3).toString();
}
function increase_d(element_id) {
	var new_value = parseFloat(document.getElementById(element_id).value) + 2.0;
	if (new_value >= 0)
		document.getElementById(element_id).value = new_value.toFixed(1).toString();
}
function decrease_d(element_id) {
	var new_value = parseFloat(document.getElementById(element_id).value) - 2.0;
	if (new_value >= 0)
		document.getElementById(element_id).value = new_value.toFixed(1).toString();
}