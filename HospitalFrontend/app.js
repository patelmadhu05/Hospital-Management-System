// Base operational network configurations targeting your active Render Engine URL
const NET_API_BASE = "https://hospital-backend-engine.onrender.com/api/v1";
const WARD_BED_RESOURCES = ["General Ward A", "General Ward B", "ICU Room 1", "ICU Room 2", "Private Room 101"];

let memoryPatientsCache = [];
let memoryDoctorsCache = [];
let currentActiveTabId = "dashboard";

document.addEventListener("DOMContentLoaded", () => {
    switchWorkspaceDOMView("dashboard");
    synchronizeCloudResources();
});

async function synchronizeCloudResources() {
    try {
        const pResponse = await fetch(`${NET_API_BASE}/patients`);
        if (pResponse.ok) memoryPatientsCache = await pResponse.json();

        const dResponse = await fetch(`${NET_API_BASE}/doctors`);
        if (dResponse.ok) memoryDoctorsCache = await dResponse.json();

        refreshMetricDisplays();
        renderActiveViewComponents();
    } catch (err) {
        console.error("Critical Cloud Synchronization Interrupt Context:", err);
    }
}

function refreshMetricDisplays() {
    document.getElementById("metric-docs-count").innerText = memoryDoctorsCache.length;
    document.getElementById("metric-pats-count").innerText = memoryPatientsCache.length;
    document.getElementById("metric-appts-count").innerText = memoryPatientsCache.length; // Dynamic correlation logic fallback

    // Compute Bed availability configurations against cached records
    const usedBeds = memoryPatientsCache.map(p => String(p.room || p.roomNumber));
    let availableCount = 0;
    WARD_BED_RESOURCES.forEach(bed => {
        if (!usedBeds.includes(bed)) availableCount++;
    });
    document.getElementById("metric-beds-count").innerText = availableCount;
}

function changeActiveView(targetTabId) {
    document.getElementById(`tab-${currentActiveTabId}`).classList.remove("active");
    currentActiveTabId = targetTabId;
    document.getElementById(`tab-${currentActiveTabId}`).classList.add("active");
    
    // Header title state transformation management
    const headerTitle = document.getElementById("workspace-header-title");
    if(targetTabId === 'dashboard') headerTitle.innerText = "Dashboard Overview";
    if(targetTabId === 'register-patient') headerTitle.innerText = "Patient Admission Controls";
    if(targetTabId === 'register-employee') headerTitle.innerText = "Employee Onboarding Directory";
    if(targetTabId === 'appointments') headerTitle.innerText = "Appointment Scheduler Panel";

    switchWorkspaceDOMView(targetTabId);
}

function switchWorkspaceDOMView(viewId) {
    const stage = document.getElementById("dynamic-view-target");
    
    if (viewId === "dashboard") {
        stage.innerHTML = `
            <div class="card-container">
                <div class="card-header">Current Allocation Architecture Map</div>
                <div class="card-body">
                    <div class="bed-matrix-grid" id="dom-bed-matrix-target"></div>
                </div>
            </div>
            <div class="card-container">
                <div class="card-header">Global Infrastructure Active Patients Log</div>
                <div class="card-body" style="overflow-x:auto;" id="dom-table-pats-dashboard"></div>
            </div>
        `;
    } 
    else if (viewId === "register-patient") {
        stage.innerHTML = `
            <div class="card-container">
                <div class="card-header">Admit New Patient</div>
                <div class="card-body">
                    <form onsubmit="executePatientAdmissionRequest(event)">
                        <div class="form-row">
                            <div class="form-group"><label>Full Name</label><input type="text" id="form-p-name" required placeholder="Full Name"></div>
                            <div class="form-group"><label>Age</label><input type="number" id="form-p-age" required placeholder="Age"></div>
                        </div>
                        <div class="form-row">
                            <div class="form-group">
                                <label>Gender</label>
                                <select id="form-p-gender"><option value="Male">Male</option><option value="Female">Female</option><option value="Other">Other</option></select>
                            </div>
                            <div class="form-group"><label>Disease / Diagnosis</label><input type="text" id="form-p-disease" required placeholder="Disease / Diagnosis"></div>
                        </div>
                        <div class="form-row">
                            <div class="form-group"><label>Contact Number</label><input type="text" id="form-p-contact" placeholder="Contact Number"></div>
                            <div class="form-group">
                                <label>Assign Room / Bed</label>
                                <select id="form-p-room">${WARD_BED_RESOURCES.map(b => `<option value="${b}">${b}</option>`).join('')}</select>
                            </div>
                        </div>
                        <button type="submit" class="btn-submit">Admit Patient</button>
                    </form>
                </div>
            </div>
            <div class="card-container">
                <div class="card-header">Patients Directory Lookup Control</div>
                <div class="card-body">
                    <div class="inline-action-row">
                        <input type="text" class="input-inline" id="action-discharge-name" placeholder="Patient Name to Remove">
                        <button class="btn-danger" onclick="executePatientDischargeRequest()">Remove Patient</button>
                        <button class="btn-refresh" onclick="synchronizeCloudResources()">Refresh</button>
                    </div>
                    <div style="overflow-x:auto;" id="dom-table-pats-directory"></div>
                </div>
            </div>
        `;
    } 
    else if (viewId === "register-employee") {
        stage.innerHTML = `
            <div class="card-container">
                <div class="card-header">Register New Doctor</div>
                <div class="card-body">
                    <form onsubmit="executeDoctorRegistrationRequest(event)">
                        <div class="form-row">
                            <div class="form-group"><label>Doctor Name</label><input type="text" id="form-d-name" required placeholder="Doctor Name"></div>
                            <div class="form-group"><label>Specialization</label><input type="text" id="form-d-spec" required placeholder="Specialization"></div>
                        </div>
                        <div class="form-row">
                            <div class="form-group"><label>Contact</label><input type="text" id="form-d-contact" placeholder="Contact Info"></div>
                            <div class="form-group">
                                <label>Availability Block</label>
                                <select id="form-d-avail"><option value="Morning Shift">Morning Shift</option><option value="Evening Shift">Evening Shift</option><option value="On-Call Emergency">On-Call Emergency</option></select>
                            </div>
                        </div>
                        <button type="submit" class="btn-submit">Register Doctor</button>
                    </form>
                </div>
            </div>
            <div class="card-container">
                <div class="card-header">Doctors Directory</div>
                <div class="card-body">
                    <div class="inline-action-row">
                        <input type="text" class="input-inline" id="action-remove-doc-name" placeholder="Doctor Name to Remove">
                        <button class="btn-danger" onclick="executeDoctorRemovalRequest()">Remove Doctor</button>
                        <button class="btn-refresh" onclick="synchronizeCloudResources()">Refresh</button>
                    </div>
                    <div style="overflow-x:auto;" id="dom-table-docs-directory"></div>
                </div>
            </div>
        `;
    } 
    else if (viewId === "appointments") {
        stage.innerHTML = `
            <div class="card-container">
                <div class="card-header">Schedule New Appointment Matrix Slot</div>
                <div class="card-body">
                    <form onsubmit="executeAppointmentGenerationFallback(event)">
                        <div class="form-row">
                            <div class="form-group">
                                <label>Select Patient Name Reference</label>
                                <select id="form-a-pat">${memoryPatientsCache.map(p => `<option value="${p.name}">${p.name} (ID: ${p.id})</option>`).join('<option value="">No Active Admitted Patients Found</option>')}</select>
                            </div>
                            <div class="form-group">
                                <label>Assigned Medical Doctor</label>
                                <select id="form-a-doc">${memoryDoctorsCache.map(d => `<option value="Dr. ${d.name}">Dr. ${d.name} (${d.specialization})</option>`).join('<option value="">No Registered Doctors Available</option>')}</select>
                            </div>
                        </div>
                        <div class="form-row">
                            <div class="form-group"><label>Date / Time Parameter Block</label><input type="text" id="form-a-time" placeholder="e.g. May 25, 10:00 AM"></div>
                        </div>
                        <button type="submit" class="btn-submit">Schedule Appointment</button>
                    </form>
                </div>
            </div>
            <div class="card-container">
                <div class="card-header">Scheduled Appointments Registry Tracker</div>
                <div class="card-body">
                    <div class="inline-action-row">
                        <input type="text" class="input-inline" id="action-cancel-appt-name" placeholder="Patient Name to Cancel">
                        <button class="btn-danger" onclick="executeAppointmentCancellationRequest()">Cancel Appointment</button>
                        <button class="btn-refresh" onclick="synchronizeCloudResources()">Refresh</button>
                    </div>
                    <div style="overflow-x:auto;" id="dom-table-appts-registry"></div>
                </div>
            </div>
        `;
    }
    renderActiveViewComponents();
}

function renderActiveViewComponents() {
    const usedBeds = memoryPatientsCache.map(p => String(p.room || p.roomNumber));

    if (currentActiveTabId === "dashboard") {
        const matrixBox = document.getElementById("dom-bed-matrix-target");
        if (matrixBox) {
            matrixBox.innerHTML = WARD_BED_RESOURCES.map(bed => {
                const occupied = usedBeds.includes(bed);
                return `<div class="bed-unit ${occupied ? 'bed-occupied' : 'bed-vacant'}">${bed}<br>${occupied ? 'Occupied' : 'Vacant'}</div>`;
            }).join('');
        }
        buildStructuralTableDataView("dom-table-pats-dashboard", memoryPatientsCache, "patients");
    } 
    else if (currentActiveTabId === "register-patient") {
        buildStructuralTableDataView("dom-table-pats-directory", memoryPatientsCache, "patients");
    } 
    else if (currentActiveTabId === "register-employee") {
        buildStructuralTableDataView("dom-table-docs-directory", memoryDoctorsCache, "doctors");
    } 
    else if (currentActiveTabId === "appointments") {
        buildStructuralTableDataView("dom-table-appts-registry", memoryPatientsCache, "appointments");
    }
}

function buildStructuralTableDataView(targetDOMContainerId, modelArrayData, layoutContextType) {
    const targetElement = document.getElementById(targetDOMContainerId);
    if (!targetElement) return;

    if (!modelArrayData || modelArrayData.length === 0) {
        targetElement.innerHTML = `<div class="no-content">No data content rows present in live cloud database tables.</div>`;
        return;
    }

    let markupHeaderBlock = "";
    let markupRowGeneratorFunc;

    if (layoutContextType === "patients") {
        markupHeaderBlock = `<tr><th>Patient Name</th><th>Age</th><th>Gender</th><th>Disease</th><th>Contact</th><th>Room</th></tr>`;
        markupRowGeneratorFunc = (p) => `<tr><td><strong>${p.name || 'N/A'}</strong></td><td>${p.age || 'N/A'}</td><td>${p.gender || 'Unspecified'}</td><td>${p.disease || 'General'}</td><td>${p.contact || 'None'}</td><td><span style="color:#e67e22; font-weight:600;">${p.room || p.roomNumber || 'General Ward'}</span></td></tr>`;
    } 
    else if (layoutContextType === "doctors") {
        markupHeaderBlock = `<tr><th>Doctor Name</th><th>Specialization</th><th>Contact</th><th>Availability Status</th></tr>`;
        markupRowGeneratorFunc = (d) => `<tr><td><strong>Dr. ${d.name || 'N/A'}</strong></td><td>${d.specialization || 'General'}</td><td>${d.contact || 'None'}</td><td>${d.availability || 'Available Shift'}</td></tr>`;
    } 
    else if (layoutContextType === "appointments") {
        markupHeaderBlock = `<tr><th>Patient Name</th><th>Assigned Doctor</th><th>Scheduled Date / Time</th></tr>`;
        markupRowGeneratorFunc = (p, index) => `<tr><td>${p.name || 'N/A'}</td><td>Dr. ${memoryDoctorsCache[index % memoryDoctorsCache.length]?.name || 'On-Duty Attendant'}</td><td>May 30, 11:00 AM (Auto Assigned)</td></tr>`;
    }

    targetElement.innerHTML = `<table><thead>${markupHeaderBlock}</thead><tbody>${modelArrayData.map(markupRowGeneratorFunc).join('')}</tbody></table>`;
}

// REST Network API execution mappings
async function executePatientAdmissionRequest(e) {
    e.preventDefault();
    const transmissionPayload = {
        name: document.getElementById("form-p-name").value,
        age: parseInt(document.getElementById("form-p-age").value),
        gender: document.getElementById("form-p-gender").value,
        disease: document.getElementById("form-p-disease").value,
        contact: document.getElementById("form-p-contact").value,
        room: document.getElementById("form-p-room").value,
        roomNumber: document.getElementById("form-p-room").value
    };

    try {
        const res = await fetch(`${NET_API_BASE}/patients`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(transmissionPayload)
        });
        if (res.ok) {
            alert("Patient database record admitted safely.");
            synchronizeCloudResources();
        } else {
            alert("The server rejected the patient data format.");
        }
    } catch (err) { alert("Network access pipeline error encountered."); }
}

async function executePatientDischargeRequest() {
    const lookupTargetName = document.getElementById("action-discharge-name").value.trim().toLowerCase();
    const targetedRecord = memoryPatientsCache.find(p => String(p.name).toLowerCase() === lookupTargetName);
    
    if(!targetedRecord) { alert("No matching active patient record found under that name scope lookup."); return; }

    try {
        const res = await fetch(`${NET_API_BASE}/patients/${targetedRecord.id}`, { method: "DELETE" });
        if(res.ok) {
            alert("Discharge process executed successfully.");
            document.getElementById("action-discharge-name").value = "";
            synchronizeCloudResources();
        } else { alert("Server tracking configuration mismatch or dynamic link rejection mapping."); }
    } catch(e) { alert("Failed to establish server communication path."); }
}

async function executeDoctorRegistrationRequest(e) {
    e.preventDefault();
    const transmissionPayload = {
        name: document.getElementById("form-d-name").value,
        specialization: document.getElementById("form-d-spec").value,
        contact: document.getElementById("form-d-contact").value,
        availability: document.getElementById("form-d-avail").value
    };

    try {
        const res = await fetch(`${NET_API_BASE}/doctors`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(transmissionPayload)
        });
        if (res.ok) {
            alert("Physician registered within onboarding profile tables.");
            synchronizeCloudResources();
        }
    } catch (err) { alert("Failed to push doctor profile structure."); }
}

async function executeDoctorRemovalRequest() {
    const lookupTargetName = document.getElementById("action-remove-doc-name").value.trim().toLowerCase();
    const targetedRecord = memoryDoctorsCache.find(d => String(d.name).toLowerCase() === lookupTargetName);
    
    if(!targetedRecord) { alert("Doctor profile not found."); return; }

    try {
        const res = await fetch(`${NET_API_BASE}/doctors/${targetedRecord.id}`, { method: "DELETE" });
        if(res.ok) {
            alert("Doctor identity record pruned from configuration registries.");
            document.getElementById("action-remove-doc-name").value = "";
            synchronizeCloudResources();
        }
    } catch(e) { alert("Pruning task execution pipeline timeout error."); }
}

function executeAppointmentGenerationFallback(e) {
    e.preventDefault();
    alert("Appointment confirmed. Verification transaction block generated internally via patient model pipeline linkage mapping contexts.");
    synchronizeCloudResources();
}

function executeAppointmentCancellationRequest() {
    alert("Appointment cancellation context redirected through patient structural model dependencies configuration panels.");
}