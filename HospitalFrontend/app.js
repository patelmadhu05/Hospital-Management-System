// Global target routing to your active Render Spring Boot framework endpoint
const API_URL = "https://hospital-backend-40gg.onrender.com/api";

// Total room capacity initialization array for tracking allocations
const TOTAL_ROOM_NUMBERS = ["101", "102", "103", "104", "105"];
let loadedPatientsCache = [];

document.addEventListener("DOMContentLoaded", () => {
    initializePlatformView();
});

function initializePlatformView() {
    refreshDataMetrics();
    loadPatientList();
}

// Queries backend database tables to update dashboard numerical displays
async function refreshDataMetrics() {
    try {
        const docRes = await fetch(`${API_URL}/doctors`);
        if (docRes.ok) {
            const doctors = await docRes.json();
            document.getElementById("count-doctors").innerText = doctors.length;
        }

        const patRes = await fetch(`${API_URL}/patients`);
        if (patRes.ok) {
            loadedPatientsCache = await patRes.json();
            document.getElementById("count-patients").innerText = loadedPatientsCache.length;
            document.getElementById("count-appointments").innerText = loadedPatientsCache.length;
            
            // Calculate bed metrics based on active allocations
            updateRoomAllocationStatus(loadedPatientsCache);
        }
    } catch (error) {
        console.error("Metrics synchronization error:", error);
    }
}

// Computes current room status grids dynamically
function updateRoomAllocationStatus(patients) {
    const allocatedRooms = patients.map(p => String(p.roomNumber || p.room));
    const gridContainer = document.getElementById("rooms-visual-grid");
    
    if(!gridContainer) return; // Guard statement if sidebar view changes
    
    gridContainer.innerHTML = "";
    let availableCount = 0;

    TOTAL_ROOM_NUMBERS.forEach(room => {
        const isOccupied = allocatedRooms.includes(room);
        const box = document.createElement("div");
        box.className = `room-box ${isOccupied ? 'room-occupied' : 'room-available'}`;
        box.innerText = `R-${room}\n${isOccupied ? 'Full' : 'Vacant'}`;
        gridContainer.appendChild(box);
        
        if(!isOccupied) availableCount++;
    });

    document.getElementById("count-beds-avail").innerText = availableCount;
}

// Fetches patient data and displays it in a table with a discharge action option
async function loadPatientList() {
    const container = document.getElementById("main-data-container");
    if(!container) return;

    container.innerHTML = "<p style='color:#7f8c8d;'>Querying patient database data...</p>";

    try {
        const response = await fetch(`${API_URL}/patients`);
        const patients = await response.json();

        if (patients.length === 0) {
            container.innerHTML = "<p style='color:#7f8c8d; padding:10px;'>No patients currently admitted inside this ward.</p>";
            return;
        }

        let html = `
            <table>
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Name</th>
                        <th>Age</th>
                        <th>Medical Record</th>
                        <th>Allocated Bed</th>
                        <th>Prescription Details</th>
                        <th>Action</th>
                    </tr>
                </thead>
                <tbody>
        `;

        patients.forEach(p => {
            html += `
                <tr>
                    <td>${p.id}</td>
                    <td><strong>${p.name}</strong></td>
                    <td>${p.age}</td>
                    <td>${p.disease || 'Observation'}</td>
                    <td><span style="color:#e67e22; font-weight:600;">Room ${p.roomNumber || p.room || 'Unassigned'}</span></td>
                    <td><em>${p.prescription || 'No routine assigned'}</em></td>
                    <td><button class="btn-discharge" onclick="dischargePatient(${p.id})">Discharge</button></td>
                </tr>
            `;
        });

        html += "</tbody></table>";
        container.innerHTML = html;
    } catch (error) {
        container.innerHTML = `<p style="color:#e74c3c;">Failed to load patient records from backend cloud link.</p>`;
    }
}

// Sends a POST request to add a new patient to the database
async function handlePatientAdmission(event) {
    event.preventDefault();

    const name = document.getElementById("pat-name").value;
    const age = parseInt(document.getElementById("pat-age").value);
    const disease = document.getElementById("pat-disease").value;
    const room = document.getElementById("pat-room").value;
    const prescription = document.getElementById("pat-prescription").value;

    // Constructs a unified JSON model mapping variables to backend encapsulation rules
    const payload = {
        name: name,
        age: age,
        disease: disease,
        roomNumber: room, // Matches standard database variable names
        room: room,       
        prescription: prescription
    };

    try {
        const response = await fetch(`${API_URL}/patients`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            alert(`Admission successful: ${name} registered.`);
            document.getElementById("admission-form").reset();
            initializePlatformView(); // Refresh table displays and calculations
        } else {
            alert("The backend rejected this entry submission structure.");
        }
    } catch (error) {
        alert("Network communication error with cloud server.");
    }
}

// Sends a DELETE request to clear an item row by its ID
async function dischargePatient(id) {
    if (!confirm("Are you sure you want to process discharge operations for this patient?")) return;

    try {
        const response = await fetch(`${API_URL}/patients/${id}`, {
            method: "DELETE"
        });

        if (response.ok) {
            alert("Discharge complete. Bed record set to vacant status.");
            initializePlatformView();
        } else {
            alert("Could not process discharge. Ensure your backend has a @DeleteMapping('{id}') controller option.");
        }
    } catch (error) {
        alert("Failed to reach cloud environment engine to trigger deletion.");
    }
}

// Handles sidebar tab navigation
function switchView(view) {
    const title = document.getElementById("view-title");
    const subtitle = document.getElementById("view-subtitle");
    const layout = document.getElementById("dynamic-layout-area");
    
    document.getElementById("menu-dashboard").classList.remove("active");
    document.getElementById("menu-doctors").classList.remove("active");

    if (view === 'dashboard') {
        document.getElementById("menu-dashboard").classList.add("active");
        title.innerText = "Dashboard Operations";
        subtitle.innerText = "Patient Admission, Bed Allocations, and Digital Prescription Procedures";
        
        // Restore double panel format
        layout.style.gridTemplateColumns = window.innerWidth > 900 ? "2fr 1fr" : "1fr";
        layout.innerHTML = `
            <div class="data-section">
                <div class="section-header" id="table-title">Currently Admitted Patients</div>
                <div id="main-data-container" style="overflow-x: auto;"></div>
            </div>
            <div id="side-panel-container" style="display: flex; flex-direction: column; gap: 20px;">
                <div class="data-section">
                    <div class="section-header">Admit New Patient</div>
                    <form id="admission-form" onsubmit="handlePatientAdmission(event)">
                        <div class="form-group"><label>Patient Name</label><input type="text" id="pat-name" required placeholder="Enter full name"></div>
                        <div class="form-group"><label>Age</label><input type="number" id="pat-age" required placeholder="Age"></div>
                        <div class="form-group"><label>Diagnosis / Medical History</label><input type="text" id="pat-disease" required placeholder="Current condition"></div>
                        <div class="form-group"><label>Bed / Room Allocation</label><select id="pat-room" required><option value="101">Room 101</option><option value="102">Room 102</option><option value="103">Room 103</option><option value="104">Room 104</option><option value="105">Room 105</option></select></div>
                        <div class="form-group"><label>Prescription Statement</label><textarea id="pat-prescription" rows="2" placeholder="Prescribed medication routines"></textarea></div>
                        <button type="submit" class="btn-action">Confirm Admission</button>
                    </form>
                </div>
                <div class="data-section">
                    <div class="section-header">Room Status Mapping</div>
                    <div class="room-grid" id="rooms-visual-grid"></div>
                </div>
            </div>
        `;
        initializePlatformView();
    } 
    else if (view === 'doctors') {
        document.getElementById("menu-doctors").classList.add("active");
        title.innerText = "Medical Staff Directory";
        subtitle.innerText = "Registered practitioners and active diagnostic specialists lookup profile view";
        
        layout.style.gridTemplateColumns = "1fr";
        layout.innerHTML = `
            <div class="data-section">
                <div class="section-header">Active Medical Staff Listings</div>
                <div id="main-data-container" style="overflow-x: auto;"></div>
            </div>
        `;
        loadStaffDirectory();
    }
}

async function loadStaffDirectory() {
    const container = document.getElementById("main-data-container");
    container.innerHTML = "<p style='color:#7f8c8d;'>Querying doctors list...</p>";
    try {
        const response = await fetch(`${API_URL}/doctors`);
        const doctors = await response.json();

        if (doctors.length === 0) {
            container.innerHTML = "<p style='color:#7f8c8d; padding:10px;'>No active doctors found inside the registry.</p>";
            return;
        }

        let html = `<table><thead><tr><th>ID</th><th>Physician Name</th><th>Specialization Department</th></tr></thead><tbody>`;
        doctors.forEach(d => {
            html += `<tr><td>${d.id}</td><td><strong>Dr. ${d.name}</strong></td><td>${d.specialization || 'General Medicine'}</td></tr>`;
        });
        html += "</tbody></table>";
        container.innerHTML = html;
    } catch (e) {
        container.innerHTML = "<p style='color:#e74c3c;'>Error linking to core medical database tables.</p>";
    }
}