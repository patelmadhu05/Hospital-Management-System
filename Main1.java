// JavaFX Core Graphics & Application Imports
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.Node;

// JavaFX Layout Panes
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.geometry.Pos;
import javafx.geometry.Insets;

// JavaFX UI Controls
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Separator;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

// JavaFX Collections & Filtering
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

// Java Network, Database & Utility Imports
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Main1 extends Application {

    private Stage primaryStage;
    private Scene loginScene;
    private Scene dashboardScene;
    
    // --- TABLE & REALTIME OBSERVABLE DATA STREAMS ---
    private TableView<Patient> patientTable;
    private ObservableList<Patient> patientList = FXCollections.observableArrayList();
    private FilteredList<Patient> filteredPatientList;

    private TableView<Doctor> doctorTable;
    private ObservableList<Doctor> doctorList = FXCollections.observableArrayList();
    private FilteredList<Doctor> filteredDoctorList;

    private TableView<Appointment> appointmentTable;
    private ObservableList<Appointment> appointmentList = FXCollections.observableArrayList();

    private ObservableList<String> patientNamesList = FXCollections.observableArrayList();
    private ObservableList<String> doctorNamesList = FXCollections.observableArrayList();

    // ================= DATA ENCAPSULATION MODELS =================
    public static class Patient {
        private final String name;
        private final int age;
        private final String gender;
        private final String disease;
        private final String contact;
        private final String roomNumber;

        public Patient(String name, int age, String gender, String disease, String contact, String roomNumber) {
            this.name = name;
            this.age = age;
            this.gender = gender;
            this.disease = disease;
            this.contact = contact;
            this.roomNumber = roomNumber;
        }

        public String getName() { return name; }
        public int getAge() { return age; }
        public String getGender() { return gender; }
        public String getDisease() { return disease; }
        public String getContact() { return contact; }
        public String getRoomNumber() { return roomNumber; }
    }

    public static class Doctor {
        private final String name;
        private final String specialization;
        private final String contact;
        private final String availability;

        public Doctor(String name, String specialization, String contact, String availability) {
            this.name = name;
            this.specialization = specialization;
            this.contact = contact;
            this.availability = availability;
        }

        public String getName() { return name; }
        public String getSpecialization() { return specialization; }
        public String getContact() { return contact; }
        public String getAvailability() { return availability; }
    }

    public static class Appointment {
        private final String patientName;
        private final String doctorName;
        private final String appointmentDate;

        public Appointment(String patientName, String doctorName, String appointmentDate) {
            this.patientName = patientName;
            this.doctorName = doctorName;
            this.appointmentDate = appointmentDate;
        }

        public String getPatientName() { return patientName; }
        public String getDoctorName() { return doctorName; }
        public String getAppointmentDate() { return appointmentDate; }
    }

    // ================= DATABASE / API OPERATIONS =================
    private void loadPatientsFromDB() {
        patientList.clear();
        patientNamesList.clear();
        try {
            Connection conn = DBConnection.getConnection();
            String sql = "SELECT * FROM patients";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String name = rs.getString("name");
                String room = "Not Assigned";
                try { room = rs.getString("room_number"); } catch (Exception ignored) {}
                if(room == null || room.isEmpty()) room = "General Ward";

                patientList.add(new Patient(name, rs.getInt("age"), rs.getString("gender"), rs.getString("disease"), rs.getString("contact"), room));
                patientNamesList.add(name);
            }
            conn.close();
        } catch (Exception e) { System.out.println("Patient fetch fail: " + e.getMessage()); }
    }

    private void loadDoctorsFromDB() {
        doctorList.clear();
        doctorNamesList.clear();
        try {
            URL url = new URL("http://localhost:8080/api/doctors");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                br.close();

                String json = response.toString().trim();
                if (json.startsWith("[") && json.endsWith("]")) {
                    json = json.substring(1, json.length() - 1).trim();
                    if (!json.isEmpty()) {
                        String[] objects = json.split("\\},\\{");
                        for (String obj : objects) {
                            obj = obj.replace("{", "").replace("}", "");
                            String name = parseJsonValue(obj, "name");
                            String spec = parseJsonValue(obj, "specialization");
                            String contact = parseJsonValue(obj, "contact");
                            String avail = parseJsonValue(obj, "availability");

                            if (name != null) {
                                doctorList.add(new Doctor(name, spec, contact, avail));
                                doctorNamesList.add(name);
                            }
                        }
                    }
                }
            }
            conn.disconnect();
        } catch (Exception e) { 
            System.out.println("Doctor API fetch fail: " + e.getMessage()); 
        }
    }

    private String parseJsonValue(String jsonBlock, String key) {
        String matchToken = "\"" + key + "\":";
        int index = jsonBlock.indexOf(matchToken);
        if (index == -1) return "";
        int start = index + matchToken.length();
        
        if (jsonBlock.charAt(start) == '"') {
            start++;
            int end = jsonBlock.indexOf("\"", start);
            return jsonBlock.substring(start, end);
        } else {
            int end = jsonBlock.indexOf(",", start);
            if (end == -1) end = jsonBlock.length();
            return jsonBlock.substring(start, end).trim();
        }
    }

    private void loadAppointmentsFromDB() {
        appointmentList.clear();
        try {
            Connection conn = DBConnection.getConnection();
            String sql = "SELECT * FROM appointments";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                appointmentList.add(new Appointment(rs.getString("patient_name"), rs.getString("doctor_name"), rs.getString("appointment_date")));
            }
            conn.close();
        } catch (Exception e) { System.out.println("Appointment fetch fail: " + e.getMessage()); }
    }

    private void refreshAllData() {
        loadPatientsFromDB();
        loadDoctorsFromDB();
        loadAppointmentsFromDB();
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        createLoginScene();
        createDashboardScene();

        primaryStage.setTitle("Hospital Management System - Login");
        primaryStage.setScene(loginScene);
        primaryStage.show();
    }

    // ===================== PORTAL STAGE 1: SECURE GATEWAY =====================
    private void createLoginScene() {
        VBox loginBox = new VBox(15.0);
        loginBox.getStyleClass().add("login-box");
        loginBox.setMaxWidth(350); loginBox.setMaxHeight(300); loginBox.setAlignment(Pos.CENTER);

        Label loginTitle = new Label("🔒 Admin Portal Login");
        loginTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        TextField usernameField = new TextField(); usernameField.setPromptText("Username");
        PasswordField passwordField = new PasswordField(); passwordField.setPromptText("Password");
        Button loginBtn = new Button("Login"); loginBtn.getStyleClass().add("login-button"); loginBtn.setMaxWidth(Double.MAX_VALUE);
        Label errorLabel = new Label(); errorLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");

        loginBox.getChildren().addAll(loginTitle, usernameField, passwordField, loginBtn, errorLabel);
        StackPane rootPane = new StackPane(loginBox); rootPane.setStyle("-fx-background-color: #e9ecef;");

        loginScene = new Scene(rootPane, 950, 600);
        loginScene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

        loginBtn.setOnAction(e -> {
            if (usernameField.getText().equals("admin") && passwordField.getText().equals("hospital123")) {
                primaryStage.setScene(dashboardScene);
                primaryStage.setTitle("Hospital Management System (Pro Version)");
                refreshAllData();
            } else {
                errorLabel.setText("Invalid username or password!");
                passwordField.clear();
            }
        });
    }

    // ===================== PORTAL STAGE 2: MANAGEMENT PLATFORM =====================
    @SuppressWarnings("unchecked")
    private void createDashboardScene() {
        Label title = new Label("🏥 Hospital Dashboard");
        title.getStyleClass().add("header-label");

        TextField searchBar = new TextField();
        searchBar.setPromptText("🔍 Global Directory Live Filter...");
        searchBar.setPrefWidth(280);

        Button logoutBtn = new Button("Logout"); logoutBtn.setStyle("-fx-background-color: #e74c3c;");
        Region spacer1 = new Region(); HBox.setHgrow(spacer1, Priority.ALWAYS);
        Region spacer2 = new Region(); HBox.setHgrow(spacer2, Priority.ALWAYS);
        
        HBox topBar = new HBox(15.0, title, spacer1, searchBar, spacer2, logoutBtn);
        topBar.setPadding(new Insets(15)); topBar.setStyle("-fx-background-color: white;"); topBar.setAlignment(Pos.CENTER_LEFT);

        TabPane mainTabPane = new TabPane();
        mainTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // ------------------------- TAB 1: PATIENTS DIRECTORY -------------------------
        Tab patientTab = new Tab("👥 Patients Directory");
        BorderPane patientPane = new BorderPane();
        
        TextField pNameField = new TextField(); pNameField.setPromptText("Name");
        TextField pAgeField = new TextField(); pAgeField.setPromptText("Age");
        ComboBox<String> pGenderBox = new ComboBox<>(FXCollections.observableArrayList("Male", "Female", "Other"));
        pGenderBox.setPromptText("Select Gender"); pGenderBox.setMaxWidth(Double.MAX_VALUE);
        TextField pDiseaseField = new TextField(); pDiseaseField.setPromptText("Disease");
        TextField pContactField = new TextField(); pContactField.setPromptText("Contact");
        
        ComboBox<String> pRoomBox = new ComboBox<>(FXCollections.observableArrayList("General Ward A", "General Ward B", "ICU Room 1", "ICU Room 2", "Private Suite 101"));
        pRoomBox.setPromptText("Assign Bed/Room"); pRoomBox.setMaxWidth(Double.MAX_VALUE);
        
        TextField pRemoveNameField = new TextField(); pRemoveNameField.setPromptText("Patient Name");

        Button pAdmitBtn = new Button("Admit Patient"); pAdmitBtn.setMaxWidth(Double.MAX_VALUE);
        Button pRemoveBtn = new Button("Remove Patient"); pRemoveBtn.setMaxWidth(Double.MAX_VALUE);
        Button pRefreshBtn = new Button("Refresh Data"); pRefreshBtn.setMaxWidth(Double.MAX_VALUE);

        Label pSectionHeader1 = new Label("Admit New Patient");
        pSectionHeader1.getStyleClass().add("section-header");
        Label pSectionHeader2 = new Label("Manage Patient");
        pSectionHeader2.getStyleClass().add("section-header");

        VBox pSideBar = new VBox(10.0, 
            pSectionHeader1, pNameField, pAgeField, pGenderBox, pDiseaseField, pContactField, pRoomBox, pAdmitBtn,
            new Separator(), pSectionHeader2, pRemoveNameField, pRemoveBtn, pRefreshBtn
        );
        pSideBar.setPadding(new Insets(15)); pSideBar.setPrefWidth(240); pSideBar.setStyle("-fx-background-color: white;");

        patientTable = new TableView<>();
        TableColumn<Patient, String> colPName = new TableColumn<>("Patient Name"); colPName.setCellValueFactory(new PropertyValueFactory<>("name")); colPName.setPrefWidth(130);
        TableColumn<Patient, Integer> colPAge = new TableColumn<>("Age"); colPAge.setCellValueFactory(new PropertyValueFactory<>("age")); colPAge.setPrefWidth(50);
        TableColumn<Patient, String> colPGender = new TableColumn<>("Gender"); colPGender.setCellValueFactory(new PropertyValueFactory<>("gender")); colPGender.setPrefWidth(80);
        TableColumn<Patient, String> colPDisease = new TableColumn<>("Disease"); colPDisease.setCellValueFactory(new PropertyValueFactory<>("disease")); colPDisease.setPrefWidth(120);
        TableColumn<Patient, String> colPContact = new TableColumn<>("Contact"); colPContact.setCellValueFactory(new PropertyValueFactory<>("contact")); colPContact.setPrefWidth(120);
        TableColumn<Patient, String> colPRoom = new TableColumn<>("Allocated Room"); colPRoom.setCellValueFactory(new PropertyValueFactory<>("roomNumber")); colPRoom.setPrefWidth(130);
        
        patientTable.getColumns().addAll(colPName, colPAge, colPGender, colPDisease, colPContact, colPRoom);
        
        filteredPatientList = new FilteredList<>(patientList, p -> true);
        patientTable.setItems(filteredPatientList);
        
        VBox patientTableContainer = new VBox(10.0, patientTable);
        patientTableContainer.setPadding(new Insets(15));
        VBox.setVgrow(patientTable, Priority.ALWAYS);
        
        patientPane.setLeft(pSideBar); patientPane.setCenter(patientTableContainer);
        patientTab.setContent(patientPane);

        // ------------------------- TAB 2: DOCTORS DIRECTORY -------------------------
        Tab docTab = new Tab("🩺 Doctors Directory");
        BorderPane docPane = new BorderPane();

        TextField dNameField = new TextField(); dNameField.setPromptText("Dr. Name");
        TextField dSpecField = new TextField(); dSpecField.setPromptText("Specialization");
        TextField dContactField = new TextField(); dContactField.setPromptText("Contact Info");
        ComboBox<String> dAvailBox = new ComboBox<>(FXCollections.observableArrayList("Morning", "Evening", "Full-Time"));
        dAvailBox.setPromptText("Availability Block"); dAvailBox.setMaxWidth(Double.MAX_VALUE);
        TextField dRemoveNameField = new TextField(); dRemoveNameField.setPromptText("Doctor Name");

        Button dAddBtn = new Button("Register Doctor"); dAddBtn.setMaxWidth(Double.MAX_VALUE);
        Button dRemoveBtn = new Button("Remove Doctor"); dRemoveBtn.setMaxWidth(Double.MAX_VALUE);
        Button dRefreshBtn = new Button("Refresh Data"); dRefreshBtn.setMaxWidth(Double.MAX_VALUE);

        Label dSectionHeader1 = new Label("Register New Doctor");
        dSectionHeader1.getStyleClass().add("section-header");
        Label dSectionHeader2 = new Label("Manage Staff");
        dSectionHeader2.getStyleClass().add("section-header");

        VBox dSideBar = new VBox(10.0, 
            dSectionHeader1, dNameField, dSpecField, dContactField, dAvailBox, dAddBtn,
            new Separator(), dSectionHeader2, dRemoveNameField, dRemoveBtn, dRefreshBtn
        );
        dSideBar.setPadding(new Insets(15)); dSideBar.setPrefWidth(240); dSideBar.setStyle("-fx-background-color: white;");

        doctorTable = new TableView<>();
        TableColumn<Doctor, String> colDName = new TableColumn<>("Doctor Name"); colDName.setCellValueFactory(new PropertyValueFactory<>("name")); colDName.setPrefWidth(180);
        TableColumn<Doctor, String> colDSpec = new TableColumn<>("Specialization"); colDSpec.setCellValueFactory(new PropertyValueFactory<>("specialization")); colDSpec.setPrefWidth(150);
        TableColumn<Doctor, String> colDContact = new TableColumn<>("Contact"); colDContact.setCellValueFactory(new PropertyValueFactory<>("contact")); colDContact.setPrefWidth(130);
        TableColumn<Doctor, String> colDAvail = new TableColumn<>("Availability"); colDAvail.setCellValueFactory(new PropertyValueFactory<>("availability")); colDAvail.setPrefWidth(120);
        doctorTable.getColumns().addAll(colDName, colDSpec, colDContact, colDAvail);
        
        filteredDoctorList = new FilteredList<>(doctorList, d -> true);
        doctorTable.setItems(filteredDoctorList);
        
        VBox docTableContainer = new VBox(10.0, doctorTable);
        docTableContainer.setPadding(new Insets(15));
        VBox.setVgrow(doctorTable, Priority.ALWAYS);
        
        docPane.setLeft(dSideBar); docPane.setCenter(docTableContainer);
        docTab.setContent(docPane);

        // ------------------------- TAB 3: BOOK APPOINTMENTS -------------------------
        Tab appointmentTab = new Tab("📅 Book Appointments");
        BorderPane appPane = new BorderPane();

        ComboBox<String> appPatientBox = new ComboBox<>(patientNamesList);
        appPatientBox.setPromptText("Select Patient"); appPatientBox.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> appDoctorBox = new ComboBox<>(doctorNamesList);
        appDoctorBox.setPromptText("Select Doctor"); appDoctorBox.setMaxWidth(Double.MAX_VALUE);

        TextField appDateField = new TextField(); appDateField.setPromptText("Date & Time (e.g. May 25, 10:00 AM)");
        TextField appRemoveField = new TextField(); appRemoveField.setPromptText("Patient Name");

        Button bookAppBtn = new Button("Schedule Appointment"); bookAppBtn.setMaxWidth(Double.MAX_VALUE);
        Button cancelAppBtn = new Button("Cancel Appointment"); cancelAppBtn.setMaxWidth(Double.MAX_VALUE);
        Button appRefreshBtn = new Button("Refresh Data"); appRefreshBtn.setMaxWidth(Double.MAX_VALUE);

        Label appSectionHeader1 = new Label("New Appointment");
        appSectionHeader1.getStyleClass().add("section-header");
        Label appSectionHeader2 = new Label("Cancel Session");
        appSectionHeader2.getStyleClass().add("section-header");

        VBox appSideBar = new VBox(10.0,
            appSectionHeader1, appPatientBox, appDoctorBox, appDateField, bookAppBtn,
            new Separator(), appSectionHeader2, appRemoveField, cancelAppBtn, appRefreshBtn
        );
        appSideBar.setPadding(new Insets(15)); appSideBar.setPrefWidth(240); appSideBar.setStyle("-fx-background-color: white;");

        appointmentTable = new TableView<>();
        TableColumn<Appointment, String> colAppPat = new TableColumn<>("Patient Name"); colAppPat.setCellValueFactory(new PropertyValueFactory<>("patientName")); colAppPat.setPrefWidth(200);
        TableColumn<Appointment, String> colAppDoc = new TableColumn<>("Assigned Doctor"); colAppDoc.setCellValueFactory(new PropertyValueFactory<>("doctorName")); colAppDoc.setPrefWidth(200);
        TableColumn<Appointment, String> colAppDate = new TableColumn<>("Appointment Date / Time"); colAppDate.setCellValueFactory(new PropertyValueFactory<>("appointmentDate")); colAppDate.setPrefWidth(220);
        appointmentTable.getColumns().addAll(colAppPat, colAppDoc, colAppDate);
        appointmentTable.setItems(appointmentList);
        
        VBox appTableContainer = new VBox(10.0, appointmentTable);
        appTableContainer.setPadding(new Insets(15));
        VBox.setVgrow(appointmentTable, Priority.ALWAYS);
        
        appPane.setLeft(appSideBar); appPane.setCenter(appTableContainer);
        appointmentTab.setContent(appPane);

        mainTabPane.getTabs().addAll(patientTab, docTab, appointmentTab);

        // ===================== SEARCH LOGIC EVENT RE-ROUTE BINDING =====================
        searchBar.textProperty().addListener((observable, oldValue, newValue) -> {
            String filter = newValue.toLowerCase();
            
            filteredPatientList.setPredicate(patient -> {
                if (newValue.isEmpty()) return true;
                return patient.getName().toLowerCase().contains(filter) || 
                       patient.getDisease().toLowerCase().contains(filter) ||
                       patient.getRoomNumber().toLowerCase().contains(filter);
            });

            filteredDoctorList.setPredicate(doctor -> {
                if (newValue.isEmpty()) return true;
                return doctor.getName().toLowerCase().contains(filter) || 
                       doctor.getSpecialization().toLowerCase().contains(filter);
            });
        });

        // ===================== EVENT ACTIONS EXECUTION PIPELINES =====================
        pAdmitBtn.setOnAction(e -> {
            String selectedGender = pGenderBox.getValue();
            String assignedRoom = pRoomBox.getValue() != null ? pRoomBox.getValue() : "General Ward";
            if (pNameField.getText().isEmpty() || pAgeField.getText().isEmpty() || selectedGender == null) return;
            try {
                Connection conn = DBConnection.getConnection();
                String sql = "INSERT INTO patients (name, age, gender, disease, contact, room_number) VALUES (?, ?, ?, ?, ?, ?)";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, pNameField.getText());
                stmt.setInt(2, Integer.parseInt(pAgeField.getText()));
                stmt.setString(3, selectedGender);
                stmt.setString(4, pDiseaseField.getText());
                stmt.setString(5, pContactField.getText());
                stmt.setString(6, assignedRoom);
                stmt.executeUpdate(); conn.close();
                pNameField.clear(); pAgeField.clear(); pGenderBox.setValue(null); pDiseaseField.clear(); pContactField.clear(); pRoomBox.setValue(null);
                refreshAllData();
            } catch (Exception ex) { System.out.println(ex.getMessage()); }
        });

        pRemoveBtn.setOnAction(e -> {
            try {
                Connection conn = DBConnection.getConnection();
                String sql = "DELETE FROM patients WHERE name = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, pRemoveNameField.getText());
                stmt.executeUpdate(); conn.close();
                pRemoveNameField.clear(); refreshAllData();
            } catch (Exception ex) { System.out.println(ex.getMessage()); }
        });
        pRefreshBtn.setOnAction(e -> refreshAllData());

        dAddBtn.setOnAction(e -> {
            if (dNameField.getText().isEmpty() || dSpecField.getText().isEmpty() || dAvailBox.getValue() == null) return;
            try {
                URL url = new URL("http://localhost:8080/api/doctors");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                String jsonPayload = String.format(
                    "{\"name\":\"%s\",\"specialization\":\"%s\",\"contact\":\"%s\",\"availability\":\"%s\"}",
                    dNameField.getText(), dSpecField.getText(), dContactField.getText(), dAvailBox.getValue()
                );

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                if (conn.getResponseCode() == 200 || conn.getResponseCode() == 201) {
                    dNameField.clear(); dSpecField.clear(); dContactField.clear(); dAvailBox.setValue(null);
                    refreshAllData();
                }
                conn.disconnect();
            } catch (Exception ex) { 
                System.out.println("Doctor API save fail: " + ex.getMessage()); 
            }
        });

        dRemoveBtn.setOnAction(e -> {
            String targetName = dRemoveNameField.getText().trim();
            if (targetName.isEmpty()) return;
            
            try {
                String encodedName = java.net.URLEncoder.encode(targetName, StandardCharsets.UTF_8.toString()).replace("+", "%20");
                URL url = new URL("http://localhost:8080/api/doctors/name/" + encodedName);
                
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("DELETE");
                conn.setRequestProperty("Accept", "application/json");

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    System.out.println("Successfully deleted doctor: " + targetName);
                    dRemoveNameField.clear();
                    refreshAllData(); 
                } else {
                    System.out.println("Backend rejection. Response Code: " + responseCode);
                }
                conn.disconnect();
            } catch (Exception ex) {
                System.out.println("Doctor API deletion failure: " + ex.getMessage());
            }
        });

        cancelAppBtn.setOnAction(e -> {
            try {
                Connection conn = DBConnection.getConnection();
                String sql = "DELETE FROM appointments WHERE patient_name = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, appRemoveField.getText());
                stmt.executeUpdate(); conn.close();
                appRemoveField.clear(); refreshAllData();
            } catch (Exception ex) { System.out.println(ex.getMessage()); }
        });
        appRefreshBtn.setOnAction(e -> refreshAllData());

        logoutBtn.setOnAction(e -> {
            primaryStage.setScene(loginScene);
            primaryStage.setTitle("Hospital Management System - Login");
        });

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(mainTabPane);

        dashboardScene = new Scene(root, 950, 600);
        dashboardScene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
    }

    public static void main(String[] args) {
        launch();
    }
}