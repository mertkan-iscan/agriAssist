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

$(document).ready(function () {
    $.ajax({
        url: "/api/plants/types",
        method: "GET",
        success: function (data) {
            const plantTypeSelect = $("#plantType");
            plantTypeSelect.empty();
            data.forEach(type => {
                plantTypeSelect.append(new Option(type, type));
            });
        },
        error: function () {
            alert("Failed to load plant types.");
        }
    });
});

$(document).ready(function () {
    // Populate plant types dynamically
    $.ajax({
        url: "/api/plants/types",
        method: "GET",
        success: function (data) {
            const plantTypeSelect = $("#plantType");
            plantTypeSelect.empty();
            data.forEach(type => {
                plantTypeSelect.append(new Option(type, type));
            });

            // Trigger stage loading when the first plant type is loaded
            if (data.length > 0) {
                loadPlantStages(data[0]);
            }
        },
        error: function () {
            alert("Failed to load plant types.");
        }
    });

    // Load plant stages dynamically based on the selected plant type
    $("#plantType").change(function () {
        const selectedPlantType = $(this).val();
        loadPlantStages(selectedPlantType);
    });
});

function loadPlantStages(plantType) {
    $.ajax({
        url: `/api/plants/${plantType}/stages`,
        method: "GET",
        success: function (data) {
            const plantStageSelect = $("#plantStage");
            plantStageSelect.empty();
            data.forEach(stage => {
                plantStageSelect.append(new Option(stage, stage));
            });
        },
        error: function () {
            alert("Failed to load plant stages.");
        }
    });
}