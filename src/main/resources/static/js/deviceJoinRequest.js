document.addEventListener('DOMContentLoaded', function() {
    // Function to handle form submission
    function submitForm(action) {
        var deviceID = document.getElementById('deviceID').value;
        var fieldID = document.getElementById('fieldID').value;

        // Prepare the request body as URL-encoded form data
        var requestBody = new URLSearchParams();
        requestBody.append('deviceID', deviceID);
        requestBody.append('action', action);

        if (action === 'accept') {
            requestBody.append('fieldID', fieldID);
        }

        // Submit form data to the server
        fetch(`/api/devices/${deviceID}/join-request`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: requestBody.toString()
        })
            .then(response => {
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
                return response.text();
            })
            .then(data => {
                console.log('Success:', data);
                $('#joinRequestModal').modal('hide');
                alert('Request processed successfully.');
            })
            .catch(error => {
                console.error('Error:', error);
                alert('An error occurred while processing the request.');
            });
    }

    function showJoinRequestPopup(device) {
        // Set device info in the form
        document.getElementById('deviceID').value = device.deviceID;
        document.getElementById('displayDeviceID').innerText = device.deviceID;
        document.getElementById('deviceType').innerText = device.deviceType;

        // Update form action URL to include deviceID
        var form = document.getElementById('joinRequestForm');
        form.setAttribute('action', `/api/devices/${device.deviceID}/join-request`);

        // Populate field dropdown
        var fieldSelect = document.getElementById('fieldID');
        fieldSelect.innerHTML = ''; // Clear previous options

        fetch('/api/fields/all')
            .then(response => response.json())
            .then(data => {
                data.forEach(field => {
                    var option = document.createElement('option');
                    option.value = field.fieldID;
                    option.text = field.fieldName;
                    fieldSelect.add(option);
                });
            })
            .catch(error => {
                console.error('Error fetching field data:', error);
            });

        // Show modal
        $('#joinRequestModal').modal('show');

        // Attach event listeners to buttons after the modal is shown
        document.getElementById('acceptButton').addEventListener('click', function() {
            submitForm('accept');
        });

        document.getElementById('rejectButton').addEventListener('click', function() {
            submitForm('reject');
        });
    }

    // WebSocket connection using SockJS and STOMP
    var socket = new SockJS('/websocket');
    var stompClient = Stomp.over(socket);

    stompClient.connect({}, function (frame) {
        console.log('Connected: ' + frame);

        // Subscribe to /topic/joinRequest
        stompClient.subscribe('/topic/joinRequest', function (deviceInfo) {
            console.log('Received join request: ', deviceInfo.body);
            // Parse the incoming device info and show popup
            showJoinRequestPopup(JSON.parse(deviceInfo.body));
        });
    });
});