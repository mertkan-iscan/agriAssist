function addPlant() {
    const fieldID = document.getElementById('fieldID').value;
    const plantData = {
        plantType: document.getElementById('plantType').value,
        plantSowDate: document.getElementById('plantSowDate').value,
        plantStage: document.getElementById('plantStage').value,
        fieldID: parseInt(fieldID)
    };

    fetch(`/api/fields/${fieldID}/add-plant`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(plantData)
    })
        .then(response => {
            if (response.ok) {
                alert("Plant added successfully");
                window.location.href = '/fields';
            } else {
                alert("Failed to add plant");
            }
        })
        .catch(error => console.error("Error:", error));
}