async function fetchPatients() {
    try {
        // Calling your LIVE Render backend engine!
        const response = await fetch('https://hospital-backend-40gg.onrender.com/api/patients');
        const patients = await response.json();

        const tableBody = document.getElementById('patient-table-body');
        tableBody.innerHTML = ''; // Clear old data

        patients.forEach(patient => {
            const row = `<tr>
                <td>${patient.name}</td>
                <td>${patient.age}</td>
                <td>${patient.disease}</td>
            </tr>`;
            tableBody.innerHTML += row;
        });
    } catch (error) {
        console.error('Error fetching patients:', error);
        alert('Failed to connect to cloud backend.');
    }
}