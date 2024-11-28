function controlActuator(event, fieldID, deviceID) {
    event.preventDefault();
    const degree = document.getElementById(`degree-${deviceID}`).value;

    fetch(`/api/fields/${fieldID}/control-actuator-test`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: new URLSearchParams({
            deviceID: deviceID,
            degree: degree
        }),
    })
        .then(response => {
            if (response.ok) {
                return response.json();
            } else {
                throw new Error(`Failed to control actuator: ${response.statusText}`);
            }
        })
        .then(data => {
            showTemporaryMessage("Success", "success", deviceID);
        })
        .catch(error => {
            showTemporaryMessage(`Error: ${error.message}`, "error", deviceID);
        });
}

function showTemporaryMessage(message, type, deviceID) {
    const responseMessage = document.getElementById(`responseMessage-${deviceID}`);
    const responseText = document.getElementById(`responseText-${deviceID}`);

    if (!responseMessage || !responseText) {
        console.error('Response message elements not found in the DOM.');
        return;
    }

    responseText.textContent = message;
    responseMessage.style.backgroundColor = type === "success" ? "#4CAF50" : "#f44336";
    responseMessage.style.display = "block";

    setTimeout(() => {
        responseMessage.style.display = "none";
    }, 3000);
}


function calibrateDevice(event, fieldID, deviceID) {
    event.preventDefault();

    const degreeElement = document.getElementById(`degree-${deviceID}`);
    const flowRateElement = document.getElementById(`flowRate-${deviceID}-calibration`);

    if (!degreeElement || !flowRateElement) {
        console.error("Degree or Flow Rate element not found.");
        showTemporaryMessage("Failed to find form inputs. Please refresh and try again.", "error");
        return;
    }

    const degree = degreeElement.value;
    const flowRate = flowRateElement.value;

    if (!degree || !flowRate) {
        showTemporaryMessage("Degree and Flow Rate are required.", "error");
        return;
    }

    fetch(`/api/fields/${fieldID}/calibrate-device`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            deviceID: deviceID,
            degree: degree,
            flowRate: flowRate,
        }),
    })
        .then(response => {
            if (response.ok) {
                return response.text();
            } else {
                throw new Error(`Failed to calibrate device: ${response.statusText}`);
            }
        })
        .then(message => {
            showTemporaryMessage(`Calibration Success: ${message}`, "success");
            // Optionally hide the calibration form
            document.getElementById(`calibrateDeviceForm-${deviceID}`).style.display = 'none';
        })
        .catch(error => {
            console.error("Error during calibration:", error);
            showTemporaryMessage("Calibration failed. Please try again.", "error");
        });
}




function scheduleIrrigation(event) {
    event.preventDefault();

    const fieldId = document.getElementById('fieldId').value;
    const flowRate = document.getElementById('flowRate').value;
    const totalWaterAmount = document.getElementById('totalWaterAmount').value;
    const startTime = document.getElementById('startTime').value;

    fetch('/api/irrigation/schedule', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            fieldId: fieldId,
            flowRate: flowRate,
            totalWaterAmount: totalWaterAmount,
            startTime: startTime,
        }),
    })
        .then(response => {
            if (response.ok) {
                alert('Irrigation scheduled successfully!');
            } else {
                throw new Error('Failed to schedule irrigation.');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert('Error scheduling irrigation: ' + error.message);
        });
}