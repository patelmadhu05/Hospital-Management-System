// Base path pointing to your live Render environment engine
const BASE_URL = "https://hospital-backend-40gg.onrender.com/api";

// Auto-run totals query on page initialization
document.addEventListener("DOMContentLoaded", () => {
    updateDashboardMetrics();
});

// Queries cloud counts to populate the dashboard matrix boxes
async function updateDashboardMetrics() {
    try {
        // Query Doctors Array
        const docRes = await fetch(`${BASE_URL}/doctors`);
        if(docRes.ok) {
            const docs = await docRes.json();
            document.getElementById("count-doctors").innerText = docs.length;
        }

        // Query Patients Array
        const patRes = await fetch(`${BASE_URL}/patients`);
        if(patRes.ok) {
            const pats = await patRes.json();
            document.getElementById("count-patients").innerText = pats.length;
        }
    } catch (err) {
        console.error("Failed to load initial metrics container totals:", err);
    }
}

// Controls active layout rendering based on user interaction states
function switchView(viewName) {
    // Update active visual tracking classes in sidebar selections
    const menuItems = document.querySelectorAll(".sidebar-menu li");
    menuItems.forEach(item => item.classList.remove("active"));
    
    const titleElement = document.getElementById("view-title");
    const container = document.getElementById("table-container");
    const tableHeader = document.getElementById("table-header");

    if (viewName === 'dashboard') {
        menuItems[0].classList.add("active");
        titleElement.innerText = "Dashboard";
        tableHeader.innerText = "Hospital Overview";
        container.innerHTML = `<p style="color: #7f8c8d; font-size: 14px;">Select a tab from the sidebar menu or click "More Info" on a card to pull records dynamically from the cloud.</p>`;
        updateDashboardMetrics();
    } 
    else if (viewName === 'doctors') {
        menuItems[1].classList.add("active");
        titleElement.innerText = "Doctors";
        tableHeader.innerText = "Active Medical Specialists";
        loadDoctorRecords();
    } 
    else if (viewName === 'patients') {
        menuItems[2].classList.add("active");
        titleElement.innerText = "Patients";
        tableHeader.innerText = "Registered Patient Profiles";
        loadPatientRecords();
    }
}

// Performs background fetch requests executing your previous Java structural lookup 
async function loadDoctorRecords() {
    const container = document.getElementById("table-container");
    container.innerHTML = "<p>Retrieving database records from Render cloud...</p>";

    try {
        const response = await fetch(`${BASE_URL}/doctors`);
        const data = await response.json();

        if(data.length === 0) {
            container.innerHTML = "<p>No doctor listings found inside your database system repository.</p>";
            return;
        }

        let tableHtml = `
            <table>
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Name</th>
                        <th>Specialization</th>
                        <th>Contact info</th>
                    </tr>
                </thead>
                <tbody>
        `;

        data.forEach(doc => {
            tableHtml += `
                <tr>
                    <td>${doc.id || 'N/A'}</td>
                    <td><strong>Dr. ${doc.name || 'Unknown'}</strong></td>
                    <td>${doc.specialization || 'General'}</td>
                    <td>${doc.contact || 'None Provided'}</td>
                </tr>
            `;
        });

        tableHtml += "</tbody></table>";
        container.innerHTML = tableHtml;
    } catch (error) {
        container.innerHTML = `<p style="color: red;">Error fetching information: ${error.message}</p>`;
    }
}

// Generates structural tracking views mapping to old Java internal models
async function loadPatientRecords() {
    const container = document.getElementById("table-container");
    container.innerHTML = "<p>Retrieving database records from Render cloud...</p>";

    try {
        const response = await fetch(`${BASE_URL}/patients`);
        const data = await response.json();

        if(data.length === 0) {
            container.innerHTML = "<p>No current patient records found inside the cloud repository.</p>";
            return;
        }

        let tableHtml = `
            <table>
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Patient Name</th>
                        <th>Age</th>
                        <th>Diagnosed Disease</th>
                        <th>Room Assignment</th>
                    </tr>
                </thead>
                <tbody>
        `;

        data.forEach(pat => {
            tableHtml += `
                <tr>
                    <td>${pat.id || 'N/A'}</td>
                    <td>${pat.name || 'Anonymous'}</td>
                    <td>${pat.age || 'Unspecified'}</td>
                    <td><span style="background: #fdfaf2; padding: 3px 6px; border: 1px solid #f39c12; border-radius: 3px;">${pat.disease || 'Observation'}</span></td>
                    <td>Room ${pat.roomNumber || pat.room || 'General Ward'}</td>
                </tr>
            `;
        });

        tableHtml += "</tbody></table>";
        container.innerHTML = tableHtml;
    } catch (error) {
        container.innerHTML = `<p style="color: red;">Error fetching information: ${error.message}</p>`;
    }
}