document.getElementById('scheduleIrrigationForm').addEventListener('submit', function(event) {
    event.preventDefault(); // Prevent form submission

    const fieldID = document.getElementById('fieldID').value;
    const totalWaterAmount = document.getElementById('totalWaterAmount').value;
    const flowRate = document.getElementById('flowRate').value;
    const irrigationDuration = document.getElementById('irrigationDuration').value;
    const irrigationStartTime = document.getElementById('irrigationStartTime').value;

    // Count how many optional fields are filled
    const filledFields = [
        totalWaterAmount ? 1 : 0,
        flowRate ? 1 : 0,
        irrigationDuration ? 1 : 0
    ].reduce((a, b) => a + b, 0);

    if (filledFields !== 2) {
        alert('Please provide exactly two out of three: Total Water Amount, Flow Rate, or Irrigation Duration.');
        return;
    }

    const payload = {
        totalWaterAmount: totalWaterAmount ? parseFloat(totalWaterAmount).toFixed(2) : null,
        flowRate: flowRate ? parseFloat(flowRate).toFixed(2) : null,
        irrigationDuration: irrigationDuration ? parseInt(irrigationDuration) : null,
        irrigationStartTime
    };

    fetch(`/api/irrigation/${fieldID}/schedule`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload)
    })
        .then(response => {
            if (!response.ok) {
                throw new Error(`Error: ${response.status}`);
            }
            return response.text();
        })
        .then(data => {
            alert('Irrigation scheduled successfully!');
            console.log(data);
        })
        .catch(error => {
            console.error('Error:', error);
            alert('Failed to schedule irrigation.');
        });
});