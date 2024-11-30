function deleteField(fieldID) {
    if (confirm('Are you sure you want to delete this field?')) {
        fetch('/api/fields/delete/' + fieldID, {
            method: 'DELETE',
            headers: {
                'Content-Type': 'application/json',
            },
        })
            .then(response => {
                if (response.ok) {
                    alert('Field deleted successfully');
                    window.location.reload(); // Reload page to reflect the deletion
                } else {
                    alert('Failed to delete field');
                }
            })
            .catch(error => console.error('Error:', error));
    }
}