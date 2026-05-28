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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.RowConstraints;
import javafx.geometry.Pos;
import javafx.geometry.Insets;

// JavaFX UI Controls
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Separator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

// JavaFX Collections & Filtering
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

// JavaFX Shapes & Paint
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

// Java Network, Database & Utility Imports
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Main1 extends Application {

    // ===================== CONSTANTS =====================
    private static final String NAVY       = "#1a2553";
    private static final String NAVY_DARK  = "#141c42";
    private static final String GOLD       = "#e8a020";
    private static final String CONTENT_BG = "#f0f2f8";
    private static final String WHITE      = "#ffffff";
    private static final String TABLE_HDR  = "#1e2d6b";
    private static final String ROW_ALT    = "#eef0f8";

    private Stage primaryStage;
    private Scene loginScene;
    private Scene dashboardScene;

    // --- TABLE & REALTIME OBSERVABLE DATA STREAMS ---
    private TableView<Patient>     patientTable;
    private ObservableList<Patient>      patientList     = FXCollections.observableArrayList();
    private FilteredList<Patient>        filteredPatientList;

    private TableView<Doctor>      doctorTable;
    private ObservableList<Doctor>       doctorList      = FXCollections.observableArrayList();
    private FilteredList<Doctor>         filteredDoctorList;

    private TableView<Appointment> appointmentTable;
    private ObservableList<Appointment>  appointmentList = FXCollections.observableArrayList();

    private ObservableList<String> patientNamesList = FXCollections.observableArrayList();
    private ObservableList<String> doctorNamesList  = FXCollections.observableArrayList();

    // ===================== CURRENT VIEW STATE =====================
    private VBox  dashboardView;
    private VBox  patientView;
    private VBox  doctorView;
    private VBox  appointmentView;
    private StackPane contentArea;

    // ===================== DATA MODELS =====================
    public static class Patient {
        private final String name, gender, disease, contact, roomNumber;
        private final int age;
        public Patient(String name, int age, String gender, String disease, String contact, String roomNumber) {
            this.name = name; this.age = age; this.gender = gender;
            this.disease = disease; this.contact = contact; this.roomNumber = roomNumber;
        }
        public String getName()      { return name; }
        public int    getAge()       { return age; }
        public String getGender()    { return gender; }
        public String getDisease()   { return disease; }
        public String getContact()   { return contact; }
        public String getRoomNumber(){ return roomNumber; }
    }

    public static class Doctor {
        private final String name, specialization, contact, availability;
        public Doctor(String name, String specialization, String contact, String availability) {
            this.name = name; this.specialization = specialization;
            this.contact = contact; this.availability = availability;
        }
        public String getName()           { return name; }
        public String getSpecialization() { return specialization; }
        public String getContact()        { return contact; }
        public String getAvailability()   { return availability; }
    }

    public static class Appointment {
        private final String patientName, doctorName, appointmentDate;
        public Appointment(String patientName, String doctorName, String appointmentDate) {
            this.patientName = patientName; this.doctorName = doctorName; this.appointmentDate = appointmentDate;
        }
        public String getPatientName()    { return patientName; }
        public String getDoctorName()     { return doctorName; }
        public String getAppointmentDate(){ return appointmentDate; }
    }

    // ===================== DB / API OPERATIONS =====================
    private void loadPatientsFromDB() {
        patientList.clear(); patientNamesList.clear();
        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM patients");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String name = rs.getString("name");
                String room = "Not Assigned";
                try { room = rs.getString("room_number"); } catch (Exception ignored) {}
                if (room == null || room.isEmpty()) room = "General Ward";
                patientList.add(new Patient(name, rs.getInt("age"), rs.getString("gender"),
                        rs.getString("disease"), rs.getString("contact"), room));
                patientNamesList.add(name);
            }
            conn.close();
        } catch (Exception e) { System.out.println("Patient fetch fail: " + e.getMessage()); }
    }

    private void loadDoctorsFromDB() {
        doctorList.clear(); doctorNamesList.clear();
        try {
            URL url = new URL("https://hospital-backend-40gg.onrender.com/api/doctors");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            if (conn.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) response.append(line);
                br.close();
                String json = response.toString().trim();
                if (json.startsWith("[") && json.endsWith("]")) {
                    json = json.substring(1, json.length() - 1).trim();
                    if (!json.isEmpty()) {
                        for (String obj : json.split("\\},\\{")) {
                            obj = obj.replace("{", "").replace("}", "");
                            String name = parseJsonValue(obj, "name");
                            if (name != null) {
                                doctorList.add(new Doctor(name, parseJsonValue(obj, "specialization"),
                                        parseJsonValue(obj, "contact"), parseJsonValue(obj, "availability")));
                                doctorNamesList.add(name);
                            }
                        }
                    }
                }
            }
            conn.disconnect();
        } catch (Exception e) { System.out.println("Doctor API fetch fail: " + e.getMessage()); }
    }

    private String parseJsonValue(String jsonBlock, String key) {
        String token = "\"" + key + "\":";
        int index = jsonBlock.indexOf(token);
        if (index == -1) return "";
        int start = index + token.length();
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
            ResultSet rs = conn.prepareStatement("SELECT * FROM appointments").executeQuery();
            while (rs.next()) {
                appointmentList.add(new Appointment(rs.getString("patient_name"),
                        rs.getString("doctor_name"), rs.getString("appointment_date")));
            }
            conn.close();
        } catch (Exception e) { System.out.println("Appointment fetch fail: " + e.getMessage()); }
    }

    private void refreshAllData() {
        loadPatientsFromDB();
        loadDoctorsFromDB();
        loadAppointmentsFromDB();
    }

    // ===================== APPLICATION ENTRY =====================
    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        createLoginScene();
        createDashboardScene();
        primaryStage.setTitle("HealthMatrix - Login");
        primaryStage.setScene(loginScene);
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(680);
        primaryStage.show();
    }

    // ===================== HELPER: NAV BUTTON =====================
    private Button createNavButton(String text, boolean active) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPadding(new Insets(10, 20, 10, 20));
        btn.setStyle(
            "-fx-background-color: " + (active ? GOLD : "transparent") + ";" +
            "-fx-text-fill: " + (active ? NAVY_DARK : "white") + ";" +
            "-fx-font-size: 13px;" +
            "-fx-font-weight: " + (active ? "bold" : "normal") + ";" +
            "-fx-cursor: hand;" +
            "-fx-background-radius: 0;"
        );
        btn.setOnMouseEntered(e -> {
            if (!btn.getStyle().contains(GOLD))
                btn.setStyle(btn.getStyle().replace("transparent", "#2a3570"));
        });
        btn.setOnMouseExited(e -> {
            if (!btn.getStyle().contains(GOLD))
                btn.setStyle(btn.getStyle().replace("#2a3570", "transparent"));
        });
        return btn;
    }

    // ===================== HELPER: STAT CARD =====================
    private VBox createStatCard(String count, String label, String bgColor, String iconText) {
        Label icon = new Label(iconText);
        icon.setStyle("-fx-font-size: 28px;");

        Label countLbl = new Label(count);
        countLbl.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label nameLbl = new Label(label);
        nameLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #555; -fx-wrap-text: true;");
        nameLbl.setMaxWidth(90);

        VBox textBox = new VBox(2, countLbl, nameLbl);
        textBox.setAlignment(Pos.CENTER_LEFT);

        HBox inner = new HBox(14, icon, textBox);
        inner.setAlignment(Pos.CENTER_LEFT);
        inner.setPadding(new Insets(16));

        VBox card = new VBox(inner);
        card.setStyle(
            "-fx-background-color: " + bgColor + ";" +
            "-fx-background-radius: 10;" +
            "-fx-min-width: 160;"
        );
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    // ===================== HELPER: SECTION TITLE =====================
    private Label sectionTitle(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1a2553;");
        return l;
    }

    // ===================== HELPER: FORM FIELD LABEL =====================
    private Label formLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 12px; -fx-text-fill: #666; -fx-font-weight: bold;");
        return l;
    }

    // ===================== PORTAL STAGE 1: LOGIN =====================
    private void createLoginScene() {
        // Left branding panel
        VBox leftPanel = new VBox(20);
        leftPanel.setStyle("-fx-background-color: " + NAVY + ";");
        leftPanel.setAlignment(Pos.CENTER);
        leftPanel.setPrefWidth(420);
        leftPanel.setPadding(new Insets(60, 40, 60, 40));

        Label brand = new Label("⊕ healthmatrix");
        brand.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label tagline = new Label("Smart Hospital Management System");
        tagline.setStyle("-fx-font-size: 14px; -fx-text-fill: #aab3d8; -fx-wrap-text: true;");
        tagline.setMaxWidth(280);
        tagline.setAlignment(Pos.CENTER);

        Label sub = new Label("Streamline your hospital operations with an all-in-one platform for patients, doctors and appointments.");
        sub.setStyle("-fx-font-size: 12px; -fx-text-fill: #7a87bb; -fx-wrap-text: true; -fx-text-alignment: center;");
        sub.setMaxWidth(280);
        sub.setAlignment(Pos.CENTER);

        leftPanel.getChildren().addAll(brand, tagline, sub);

        // Right form panel
        VBox rightPanel = new VBox(16);
        rightPanel.setAlignment(Pos.CENTER);
        rightPanel.setStyle("-fx-background-color: " + CONTENT_BG + ";");
        rightPanel.setPadding(new Insets(60, 60, 60, 60));

        Label loginTitle = new Label("Welcome Back");
        loginTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + NAVY + ";");

        Label loginSub = new Label("Sign in to your admin account");
        loginSub.setStyle("-fx-font-size: 13px; -fx-text-fill: #888;");

        Label userLbl = formLabel("Username");
        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter username");
        usernameField.setMaxWidth(320);
        usernameField.setStyle("-fx-background-color: white; -fx-border-color: #dde2f0; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 12; -fx-font-size: 13px;");

        Label passLbl = formLabel("Password");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter password");
        passwordField.setMaxWidth(320);
        passwordField.setStyle("-fx-background-color: white; -fx-border-color: #dde2f0; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 12; -fx-font-size: 13px;");

        Button loginBtn = new Button("Sign In");
        loginBtn.setMaxWidth(320);
        loginBtn.setPadding(new Insets(10));
        loginBtn.setStyle(
            "-fx-background-color: " + NAVY + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 6;" +
            "-fx-cursor: hand;"
        );

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");

        rightPanel.getChildren().addAll(loginTitle, loginSub, new Region(),
                userLbl, usernameField, passLbl, passwordField, loginBtn, errorLabel);

        HBox loginRoot = new HBox(leftPanel, rightPanel);
        HBox.setHgrow(rightPanel, Priority.ALWAYS);
        loginScene = new Scene(loginRoot, 1100, 680);
        try { loginScene.getStylesheets().add(getClass().getResource("style.css").toExternalForm()); } catch (Exception ignored) {}

        loginBtn.setOnAction(e -> {
            if (usernameField.getText().equals("admin") && passwordField.getText().equals("hospital123")) {
                primaryStage.setScene(dashboardScene);
                primaryStage.setTitle("HealthMatrix - Hospital Management");
                refreshAllData();
            } else {
                errorLabel.setText("Invalid username or password. Please try again.");
                passwordField.clear();
            }
        });
        passwordField.setOnAction(e -> loginBtn.fire());
    }

    // ===================== PORTAL STAGE 2: DASHBOARD =====================
    @SuppressWarnings("unchecked")
    private void createDashboardScene() {

        // ---- SIDEBAR ----
        VBox sidebar = new VBox(0);
        sidebar.setPrefWidth(210);
        sidebar.setStyle("-fx-background-color: " + NAVY + ";");

        // Logo
        HBox logoBox = new HBox(8);
        logoBox.setPadding(new Insets(18, 20, 18, 20));
        logoBox.setAlignment(Pos.CENTER_LEFT);
        Label logoIcon = new Label("⊕");
        logoIcon.setStyle("-fx-font-size: 20px; -fx-text-fill: " + GOLD + ";");
        Label logoText = new Label("healthmatrix");
        logoText.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: white;");
        logoBox.getChildren().addAll(logoIcon, logoText);

        Separator logoDivider = new Separator();
        logoDivider.setStyle("-fx-background-color: #2a3570;");

        // Nav items
        Button navDashboard   = createNavButton("  Dashboard",           true);
        Button navHospital    = createNavButton("  Hospital",            false);
        Button navRegPatient  = createNavButton("  Register Patient",    false);
        Button navRegEmployee = createNavButton("  Register Employee",   false);
        Button navLoginRoles  = createNavButton("  Login & Roles",       false);
        Button navMenuPages   = createNavButton("  Menu & Pages",        false);
        Button navRolling     = createNavButton("  Rolling Back Role",   false);
        Button navClinical    = createNavButton("  Clinical Setting",    false);
        Button navPatient     = createNavButton("  Patient",             false);
        Button navPanels      = createNavButton("  Panel & Packages",    false);
        Button navDiscount    = createNavButton("  Discount Policy",     false);
        Button navDuty        = createNavButton("  Duty Roaster",        false);

        Region sidespacer = new Region();
        VBox.setVgrow(sidespacer, Priority.ALWAYS);
        Label footerTxt = new Label("© 2022 All rights reserved.");
        footerTxt.setStyle("-fx-font-size: 10px; -fx-text-fill: #7a87bb;");
        footerTxt.setPadding(new Insets(12, 20, 12, 20));

        sidebar.getChildren().addAll(
            logoBox, logoDivider,
            navDashboard, navHospital, navRegPatient, navRegEmployee,
            navLoginRoles, navMenuPages, navRolling, navClinical,
            navPatient, navPanels, navDiscount, navDuty,
            sidespacer, footerTxt
        );

        // ---- TOP BAR ----
        TextField globalSearch = new TextField();
        globalSearch.setPromptText("  Search in app..");
        globalSearch.setPrefWidth(320);
        globalSearch.setStyle("-fx-background-color: white; -fx-border-color: #dde2f0; -fx-border-radius: 20; -fx-background-radius: 20; -fx-padding: 7 14; -fx-font-size: 13px;");

        Label chatIcon   = new Label("💬");
        Label clockIcon  = new Label("🕐");
        Label bellIcon   = new Label("🔔");
        Label notifBadge = new Label("2");
        notifBadge.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 9px; -fx-background-radius: 8; -fx-padding: 1 4;");
        StackPane bellStack = new StackPane(bellIcon, notifBadge);
        StackPane.setAlignment(notifBadge, Pos.TOP_RIGHT);

        for (Label ic : new Label[]{chatIcon, clockIcon, bellIcon})
            ic.setStyle("-fx-font-size: 18px; -fx-cursor: hand;");

        Circle avatar = new Circle(18);
        avatar.setFill(Color.web("#4a90d9"));
        Label avatarLbl = new Label("DH");
        avatarLbl.setStyle("-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");
        StackPane avatarStack = new StackPane(avatar, avatarLbl);

        Label welcomeLbl = new Label("Welcome Doctor Hashim ▾");
        welcomeLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #333; -fx-cursor: hand;");

        Button logoutBtn = new Button("Logout");
        logoutBtn.setStyle(
            "-fx-background-color: #e74c3c;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 12px;" +
            "-fx-background-radius: 5;" +
            "-fx-cursor: hand;" +
            "-fx-padding: 5 12;"
        );

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        HBox topBar = new HBox(12, globalSearch, topSpacer, chatIcon, clockIcon, bellStack,
                new Separator(), avatarStack, welcomeLbl, logoutBtn);
        topBar.setPadding(new Insets(12, 20, 12, 20));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color: white; -fx-border-color: #e8ecf2; -fx-border-width: 0 0 1 0;");

        // ---- CONTENT AREA (switchable views) ----
        contentArea = new StackPane();
        VBox.setVgrow(contentArea, Priority.ALWAYS);

        dashboardView    = buildDashboardView();
        patientView      = buildPatientView();
        doctorView       = buildDoctorView();
        appointmentView  = buildAppointmentView();

        contentArea.getChildren().addAll(appointmentView, doctorView, patientView, dashboardView);
        showView(dashboardView);

        // Nav routing
        navDashboard.setOnAction(e -> { setActiveNav(navDashboard, new Button[]{navHospital, navRegPatient, navRegEmployee, navLoginRoles, navMenuPages, navRolling, navClinical, navPatient, navPanels, navDiscount, navDuty}); showView(dashboardView); });
        navRegPatient.setOnAction(e -> { setActiveNav(navRegPatient, new Button[]{navDashboard, navHospital, navRegEmployee, navLoginRoles, navMenuPages, navRolling, navClinical, navPatient, navPanels, navDiscount, navDuty}); showView(patientView); refreshAllData(); });
        navRegEmployee.setOnAction(e -> { setActiveNav(navRegEmployee, new Button[]{navDashboard, navHospital, navRegPatient, navLoginRoles, navMenuPages, navRolling, navClinical, navPatient, navPanels, navDiscount, navDuty}); showView(doctorView); refreshAllData(); });
        navPatient.setOnAction(e -> { setActiveNav(navPatient, new Button[]{navDashboard, navHospital, navRegPatient, navRegEmployee, navLoginRoles, navMenuPages, navRolling, navClinical, navPanels, navDiscount, navDuty}); showView(appointmentView); refreshAllData(); });

        // Search filtering
        globalSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            String f = newVal.toLowerCase();
            filteredPatientList.setPredicate(p -> newVal.isEmpty() ||
                    p.getName().toLowerCase().contains(f) || p.getDisease().toLowerCase().contains(f));
            filteredDoctorList.setPredicate(d -> newVal.isEmpty() ||
                    d.getName().toLowerCase().contains(f) || d.getSpecialization().toLowerCase().contains(f));
        });

        logoutBtn.setOnAction(e -> {
            primaryStage.setScene(loginScene);
            primaryStage.setTitle("HealthMatrix - Login");
        });

        ScrollPane contentScroll = new ScrollPane(contentArea);
        contentScroll.setFitToWidth(true);
        contentScroll.setFitToHeight(true);
        contentScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox rightSide = new VBox(topBar, contentScroll);
        VBox.setVgrow(contentScroll, Priority.ALWAYS);

        BorderPane root = new BorderPane();
        root.setLeft(sidebar);
        root.setCenter(rightSide);
        root.setStyle("-fx-background-color: " + CONTENT_BG + ";");

        dashboardScene = new Scene(root, 1100, 680);
        try { dashboardScene.getStylesheets().add(getClass().getResource("style.css").toExternalForm()); } catch (Exception ignored) {}
    }

    // ---- Helper: switch active nav highlight ----
    private void setActiveNav(Button active, Button[] others) {
        active.setStyle(
            "-fx-background-color: " + GOLD + ";" +
            "-fx-text-fill: " + NAVY_DARK + ";" +
            "-fx-font-size: 13px; -fx-font-weight: bold;" +
            "-fx-cursor: hand; -fx-background-radius: 0;" +
            "-fx-alignment: CENTER_LEFT; -fx-max-width: Infinity;"
        );
        for (Button b : others) {
            b.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 13px; -fx-font-weight: normal;" +
                "-fx-cursor: hand; -fx-background-radius: 0;" +
                "-fx-alignment: CENTER_LEFT; -fx-max-width: Infinity;"
            );
        }
    }

    // ---- Helper: show a specific view ----
    private void showView(VBox view) {
        for (Node n : contentArea.getChildren()) n.setVisible(false);
        view.setVisible(true);
    }

    // ===================== VIEW: DASHBOARD =====================
    private VBox buildDashboardView() {
        VBox view = new VBox(20);
        view.setPadding(new Insets(24));
        view.setStyle("-fx-background-color: " + CONTENT_BG + ";");

        // Title row
        Label pageTitle = new Label("Dashboard");
        pageTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + NAVY + ";");

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        TextField fromDate = new TextField(today);
        fromDate.setStyle("-fx-background-color: white; -fx-border-color: #dde2f0; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 5 10; -fx-pref-width: 130px;");
        TextField toDate = new TextField(today);
        toDate.setStyle(fromDate.getStyle());
        Label fromLbl = new Label("From"); fromLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #555;");
        Label toLbl   = new Label("To");   toLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #555;");
        Region titleSpacer = new Region(); HBox.setHgrow(titleSpacer, Priority.ALWAYS);
        HBox titleRow = new HBox(10, pageTitle, titleSpacer, fromLbl, fromDate, toLbl, toDate);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        // Stat cards
        HBox statsRow = new HBox(12,
            createStatCard("17", "Waiting Patients",     "#e8f4f8", "🧍"),
            createStatCard("04", "Referred Patients",    "#fef6ea", "👥"),
            createStatCard("25", "Attended Patients",    "#fdecea", "✅"),
            createStatCard("10", "Admitted Patients",    "#f3f0fb", "🛏"),
            createStatCard("03", "Under Observation",    "#edf7ef", "🔬")
        );
        statsRow.setFillHeight(true);

        // Waiting list table
        Label waitingTitle = new Label("Waiting List");
        waitingTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + NAVY + ";");
        TextField waitingSearch = new TextField();
        waitingSearch.setPromptText("Search");
        waitingSearch.setStyle("-fx-background-color: white; -fx-border-color: #dde2f0; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 5 10; -fx-pref-width: 200px;");
        Region wSpacer = new Region(); HBox.setHgrow(wSpacer, Priority.ALWAYS);
        HBox waitingHeader = new HBox(10, waitingTitle, wSpacer, waitingSearch);
        waitingHeader.setAlignment(Pos.CENTER_LEFT);

        TableView<Patient> waitingTable = new TableView<>(filteredPatientList != null ? filteredPatientList : FXCollections.observableArrayList());
        waitingTable.setStyle("-fx-background-color: white; -fx-background-radius: 8;");
        waitingTable.setPrefHeight(300);
        waitingTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Patient, String> srCol     = styledCol("Sr.",            "name",       50);
        TableColumn<Patient, String> nameCol   = styledCol("Name",           "name",       130);
        TableColumn<Patient, String> genderCol = styledCol("Gender",         "gender",     80);
        TableColumn<Patient, Integer> ageCol   = new TableColumn<>("Age");
        ageCol.setCellValueFactory(new PropertyValueFactory<>("age")); ageCol.setPrefWidth(55);
        TableColumn<Patient, String> diseaseCol = styledCol("Category",      "disease",    110);
        TableColumn<Patient, String> contactCol = styledCol("Contact",       "contact",    120);
        TableColumn<Patient, String> roomCol    = styledCol("Access by Doctor","roomNumber",140);

        styleTableHeader(waitingTable);
        waitingTable.getColumns().addAll(srCol, nameCol, genderCol, ageCol, diseaseCol, contactCol, roomCol);

        // Pagination bar
        HBox pagination = buildPaginationBar();

        VBox tableCard = new VBox(12, waitingHeader, waitingTable, pagination);
        tableCard.setPadding(new Insets(16));
        tableCard.setStyle("-fx-background-color: white; -fx-background-radius: 10;");

        view.getChildren().addAll(titleRow, statsRow, tableCard);
        return view;
    }

    // ===================== VIEW: PATIENTS DIRECTORY =====================
    @SuppressWarnings("unchecked")
    private VBox buildPatientView() {
        VBox view = new VBox(20);
        view.setPadding(new Insets(24));
        view.setStyle("-fx-background-color: " + CONTENT_BG + ";");

        Label pageTitle = new Label("Register Patient");
        pageTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + NAVY + ";");

        // Form card
        TextField pNameField    = styledField("Full Name");
        TextField pAgeField     = styledField("Age");
        ComboBox<String> pGenderBox = new ComboBox<>(FXCollections.observableArrayList("Male", "Female", "Other"));
        pGenderBox.setPromptText("Select Gender"); styleCombo(pGenderBox);
        TextField pDiseaseField = styledField("Disease / Diagnosis");
        TextField pContactField = styledField("Contact Number");
        ComboBox<String> pRoomBox = new ComboBox<>(FXCollections.observableArrayList(
                "General Ward A", "General Ward B", "ICU Room 1", "ICU Room 2", "Private Suite 101"));
        pRoomBox.setPromptText("Assign Room / Bed"); styleCombo(pRoomBox);

        Button pAdmitBtn = actionButton("Admit Patient", NAVY);
        pAdmitBtn.setMaxWidth(Double.MAX_VALUE);

        GridPane formGrid = new GridPane();
        formGrid.setHgap(16); formGrid.setVgap(10);
        formGrid.addRow(0, formLabel("Full Name"),    pNameField,    formLabel("Age"),     pAgeField);
        formGrid.addRow(1, formLabel("Gender"),       pGenderBox,    formLabel("Disease"), pDiseaseField);
        formGrid.addRow(2, formLabel("Contact"),      pContactField, formLabel("Room"),    pRoomBox);
        for (int i = 0; i < 4; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(i % 2 == 0 ? Priority.NEVER : Priority.ALWAYS); formGrid.getColumnConstraints().add(cc);
        }

        VBox formCard = new VBox(12, sectionTitle("Admit New Patient"), formGrid, pAdmitBtn);
        formCard.setPadding(new Insets(16));
        formCard.setStyle("-fx-background-color: white; -fx-background-radius: 10;");

        // Remove row
        TextField pRemoveNameField = styledField("Patient Name to Remove");
        Button pRemoveBtn  = actionButton("Remove Patient", "#e74c3c");
        Button pRefreshBtn = actionButton("Refresh",        "#2ecc71");
        HBox removeRow = new HBox(10, pRemoveNameField, pRemoveBtn, pRefreshBtn);
        removeRow.setAlignment(Pos.CENTER_LEFT);

        // Patient table
        patientTable = new TableView<>();
        patientTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        patientTable.setPrefHeight(280);
        styleTableHeader(patientTable);

        TableColumn<Patient, String>  colPName    = styledCol("Patient Name", "name",       140);
        TableColumn<Patient, Integer> colPAge     = new TableColumn<>("Age");
        colPAge.setCellValueFactory(new PropertyValueFactory<>("age")); colPAge.setPrefWidth(55);
        TableColumn<Patient, String>  colPGender  = styledCol("Gender",    "gender",    80);
        TableColumn<Patient, String>  colPDisease = styledCol("Disease",   "disease",   120);
        TableColumn<Patient, String>  colPContact = styledCol("Contact",   "contact",   120);
        TableColumn<Patient, String>  colPRoom    = styledCol("Room",      "roomNumber",120);
        patientTable.getColumns().addAll(colPName, colPAge, colPGender, colPDisease, colPContact, colPRoom);

        filteredPatientList = new FilteredList<>(patientList, p -> true);
        patientTable.setItems(filteredPatientList);

        VBox tableCard = new VBox(12, sectionTitle("Patients Directory"), removeRow, patientTable, buildPaginationBar());
        tableCard.setPadding(new Insets(16));
        tableCard.setStyle("-fx-background-color: white; -fx-background-radius: 10;");

        view.getChildren().addAll(pageTitle, formCard, tableCard);

        // Events
        pAdmitBtn.setOnAction(e -> {
            if (pNameField.getText().isEmpty() || pAgeField.getText().isEmpty() || pGenderBox.getValue() == null) return;
            try {
                Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO patients (name, age, gender, disease, contact, room_number) VALUES (?, ?, ?, ?, ?, ?)");
                stmt.setString(1, pNameField.getText()); stmt.setInt(2, Integer.parseInt(pAgeField.getText()));
                stmt.setString(3, pGenderBox.getValue()); stmt.setString(4, pDiseaseField.getText());
                stmt.setString(5, pContactField.getText());
                stmt.setString(6, pRoomBox.getValue() != null ? pRoomBox.getValue() : "General Ward");
                stmt.executeUpdate(); conn.close();
                pNameField.clear(); pAgeField.clear(); pGenderBox.setValue(null);
                pDiseaseField.clear(); pContactField.clear(); pRoomBox.setValue(null);
                refreshAllData();
            } catch (Exception ex) { System.out.println(ex.getMessage()); }
        });

        pRemoveBtn.setOnAction(e -> {
            if (pRemoveNameField.getText().isEmpty()) return;
            try {
                Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement("DELETE FROM patients WHERE name = ?");
                stmt.setString(1, pRemoveNameField.getText());
                stmt.executeUpdate(); conn.close();
                pRemoveNameField.clear(); refreshAllData();
            } catch (Exception ex) { System.out.println(ex.getMessage()); }
        });

        pRefreshBtn.setOnAction(e -> refreshAllData());
        return view;
    }

    // ===================== VIEW: DOCTORS DIRECTORY =====================
    @SuppressWarnings("unchecked")
    private VBox buildDoctorView() {
        VBox view = new VBox(20);
        view.setPadding(new Insets(24));
        view.setStyle("-fx-background-color: " + CONTENT_BG + ";");

        Label pageTitle = new Label("Register Employee");
        pageTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + NAVY + ";");

        TextField dNameField    = styledField("Doctor Name");
        TextField dSpecField    = styledField("Specialization");
        TextField dContactField = styledField("Contact Info");
        ComboBox<String> dAvailBox = new ComboBox<>(FXCollections.observableArrayList("Morning", "Evening", "Full-Time"));
        dAvailBox.setPromptText("Availability Block"); styleCombo(dAvailBox);

        Button dAddBtn = actionButton("Register Doctor", NAVY);
        dAddBtn.setMaxWidth(Double.MAX_VALUE);

        GridPane formGrid = new GridPane();
        formGrid.setHgap(16); formGrid.setVgap(10);
        formGrid.addRow(0, formLabel("Doctor Name"),    dNameField,    formLabel("Specialization"), dSpecField);
        formGrid.addRow(1, formLabel("Contact"),        dContactField, formLabel("Availability"),   dAvailBox);
        for (int i = 0; i < 4; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(i % 2 == 0 ? Priority.NEVER : Priority.ALWAYS); formGrid.getColumnConstraints().add(cc);
        }

        VBox formCard = new VBox(12, sectionTitle("Register New Doctor"), formGrid, dAddBtn);
        formCard.setPadding(new Insets(16));
        formCard.setStyle("-fx-background-color: white; -fx-background-radius: 10;");

        TextField dRemoveNameField = styledField("Doctor Name to Remove");
        Button dRemoveBtn  = actionButton("Remove Doctor", "#e74c3c");
        Button dRefreshBtn = actionButton("Refresh",       "#2ecc71");
        HBox removeRow = new HBox(10, dRemoveNameField, dRemoveBtn, dRefreshBtn);
        removeRow.setAlignment(Pos.CENTER_LEFT);

        doctorTable = new TableView<>();
        doctorTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        doctorTable.setPrefHeight(280);
        styleTableHeader(doctorTable);

        TableColumn<Doctor, String> colDName  = styledCol("Doctor Name",    "name",           180);
        TableColumn<Doctor, String> colDSpec  = styledCol("Specialization", "specialization", 160);
        TableColumn<Doctor, String> colDCont  = styledCol("Contact",        "contact",        130);
        TableColumn<Doctor, String> colDAvail = styledCol("Availability",   "availability",   120);
        doctorTable.getColumns().addAll(colDName, colDSpec, colDCont, colDAvail);

        filteredDoctorList = new FilteredList<>(doctorList, d -> true);
        doctorTable.setItems(filteredDoctorList);

        VBox tableCard = new VBox(12, sectionTitle("Doctors Directory"), removeRow, doctorTable, buildPaginationBar());
        tableCard.setPadding(new Insets(16));
        tableCard.setStyle("-fx-background-color: white; -fx-background-radius: 10;");

        view.getChildren().addAll(pageTitle, formCard, tableCard);

        dAddBtn.setOnAction(e -> {
            if (dNameField.getText().isEmpty() || dSpecField.getText().isEmpty() || dAvailBox.getValue() == null) return;
            try {
                URL url = new URL("https://hospital-backend-40gg.onrender.com/api/doctors");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                String json = String.format("{\"name\":\"%s\",\"specialization\":\"%s\",\"contact\":\"%s\",\"availability\":\"%s\"}",
                        dNameField.getText(), dSpecField.getText(), dContactField.getText(), dAvailBox.getValue());
                try (OutputStream os = conn.getOutputStream()) { os.write(json.getBytes(StandardCharsets.UTF_8)); }
                if (conn.getResponseCode() == 200 || conn.getResponseCode() == 201) {
                    dNameField.clear(); dSpecField.clear(); dContactField.clear(); dAvailBox.setValue(null);
                    refreshAllData();
                }
                conn.disconnect();
            } catch (Exception ex) { System.out.println("Doctor API save fail: " + ex.getMessage()); }
        });

        dRemoveBtn.setOnAction(e -> {
            String name = dRemoveNameField.getText().trim();
            if (name.isEmpty()) return;
            try {
                String enc = java.net.URLEncoder.encode(name, StandardCharsets.UTF_8.toString()).replace("+", "%20");
                HttpURLConnection conn = (HttpURLConnection) new URL("https://hospital-backend-40gg.onrender.com/api/doctors/name/" + enc).openConnection();
                conn.setRequestMethod("DELETE");
                if (conn.getResponseCode() == 200) { dRemoveNameField.clear(); refreshAllData(); }
                conn.disconnect();
            } catch (Exception ex) { System.out.println("Doctor API deletion failure: " + ex.getMessage()); }
        });

        dRefreshBtn.setOnAction(e -> refreshAllData());
        return view;
    }

    // ===================== VIEW: APPOINTMENTS =====================
    @SuppressWarnings("unchecked")
    private VBox buildAppointmentView() {
        VBox view = new VBox(20);
        view.setPadding(new Insets(24));
        view.setStyle("-fx-background-color: " + CONTENT_BG + ";");

        Label pageTitle = new Label("Book Appointments");
        pageTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + NAVY + ";");

        ComboBox<String> appPatientBox = new ComboBox<>(patientNamesList);
        appPatientBox.setPromptText("Select Patient"); styleCombo(appPatientBox);
        ComboBox<String> appDoctorBox = new ComboBox<>(doctorNamesList);
        appDoctorBox.setPromptText("Select Doctor"); styleCombo(appDoctorBox);
        TextField appDateField = styledField("Date & Time (e.g. May 25, 10:00 AM)");

        Button bookAppBtn = actionButton("Schedule Appointment", NAVY);
        bookAppBtn.setMaxWidth(Double.MAX_VALUE);

        GridPane formGrid = new GridPane();
        formGrid.setHgap(16); formGrid.setVgap(10);
        formGrid.addRow(0, formLabel("Patient"),  appPatientBox, formLabel("Doctor"), appDoctorBox);
        formGrid.addRow(1, formLabel("Date / Time"), appDateField);
        for (int i = 0; i < 4; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(i % 2 == 0 ? Priority.NEVER : Priority.ALWAYS); formGrid.getColumnConstraints().add(cc);
        }

        VBox formCard = new VBox(12, sectionTitle("New Appointment"), formGrid, bookAppBtn);
        formCard.setPadding(new Insets(16));
        formCard.setStyle("-fx-background-color: white; -fx-background-radius: 10;");

        TextField appRemoveField = styledField("Patient Name to Cancel");
        Button cancelAppBtn  = actionButton("Cancel Appointment", "#e74c3c");
        Button appRefreshBtn = actionButton("Refresh",            "#2ecc71");
        HBox removeRow = new HBox(10, appRemoveField, cancelAppBtn, appRefreshBtn);
        removeRow.setAlignment(Pos.CENTER_LEFT);

        appointmentTable = new TableView<>();
        appointmentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        appointmentTable.setPrefHeight(260);
        styleTableHeader(appointmentTable);

        TableColumn<Appointment, String> colAppPat  = styledCol("Patient Name",    "patientName",    200);
        TableColumn<Appointment, String> colAppDoc  = styledCol("Assigned Doctor", "doctorName",     200);
        TableColumn<Appointment, String> colAppDate = styledCol("Date / Time",     "appointmentDate",220);
        appointmentTable.getColumns().addAll(colAppPat, colAppDoc, colAppDate);
        appointmentTable.setItems(appointmentList);

        VBox tableCard = new VBox(12, sectionTitle("Scheduled Appointments"), removeRow, appointmentTable, buildPaginationBar());
        tableCard.setPadding(new Insets(16));
        tableCard.setStyle("-fx-background-color: white; -fx-background-radius: 10;");

        view.getChildren().addAll(pageTitle, formCard, tableCard);

        bookAppBtn.setOnAction(e -> {
            if (appPatientBox.getValue() == null || appDoctorBox.getValue() == null || appDateField.getText().isEmpty()) return;
            try {
                Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO appointments (patient_name, doctor_name, appointment_date) VALUES (?, ?, ?)");
                stmt.setString(1, appPatientBox.getValue());
                stmt.setString(2, appDoctorBox.getValue());
                stmt.setString(3, appDateField.getText());
                stmt.executeUpdate(); conn.close();
                appPatientBox.setValue(null); appDoctorBox.setValue(null); appDateField.clear();
                refreshAllData();
            } catch (Exception ex) { System.out.println(ex.getMessage()); }
        });

        cancelAppBtn.setOnAction(e -> {
            if (appRemoveField.getText().isEmpty()) return;
            try {
                Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement("DELETE FROM appointments WHERE patient_name = ?");
                stmt.setString(1, appRemoveField.getText());
                stmt.executeUpdate(); conn.close();
                appRemoveField.clear(); refreshAllData();
            } catch (Exception ex) { System.out.println(ex.getMessage()); }
        });

        appRefreshBtn.setOnAction(e -> refreshAllData());
        return view;
    }

    // ===================== STYLE HELPERS =====================
    private <S, T> TableColumn<S, T> styledCol(String title, String property, double width) {
        TableColumn<S, T> col = new TableColumn<>(title);
        col.setCellValueFactory(new PropertyValueFactory<>(property));
        col.setPrefWidth(width);
        return col;
    }

    private <T> void styleTableHeader(TableView<T> table) {
        table.setStyle(
            "-fx-background-color: white;" +
            "-fx-table-header-border-color: transparent;" +
            "-fx-faint-focus-color: transparent;" +
            "-fx-focus-color: transparent;"
        );
        // Header styling via CSS — inline style for column header background
        table.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            if (newSkin != null) {
                table.lookup(".column-header-background").setStyle(
                    "-fx-background-color: " + TABLE_HDR + ";"
                );
                table.lookupAll(".column-header").forEach(n ->
                    n.setStyle("-fx-background-color: " + TABLE_HDR + "; -fx-text-fill: white;")
                );
                table.lookupAll(".label").forEach(n -> {
                    if (n.getParent() != null && n.getParent().getStyleClass().contains("column-header"))
                        n.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                });
            }
        });
    }

    private TextField styledField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle("-fx-background-color: white; -fx-border-color: #dde2f0; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 7 12; -fx-font-size: 13px;");
        return tf;
    }

    private void styleCombo(ComboBox<?> cb) {
        cb.setMaxWidth(Double.MAX_VALUE);
        cb.setStyle("-fx-background-color: white; -fx-border-color: #dde2f0; -fx-border-radius: 5; -fx-background-radius: 5; -fx-font-size: 13px;");
    }

    private Button actionButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle(
            "-fx-background-color: " + color + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 13px;" +
            "-fx-background-radius: 6;" +
            "-fx-cursor: hand;" +
            "-fx-padding: 8 16;"
        );
        return btn;
    }

    private HBox buildPaginationBar() {
        Label showing = new Label("Showing 1 to 3 of 3 entries");
        showing.setStyle("-fx-font-size: 12px; -fx-text-fill: #888;");
        Region pSpacer = new Region(); HBox.setHgrow(pSpacer, Priority.ALWAYS);

        Button[] pageButtons = { new Button("First"), new Button("Previous"),
                                 new Button("1"), new Button("2"), new Button("3"),
                                 new Button("4"), new Button("5"),
                                 new Button("Next"), new Button("Last") };
        HBox pageBox = new HBox(4);
        for (Button pb : pageButtons) {
            pb.setStyle(
                "-fx-background-color: " + ("1".equals(pb.getText()) ? TABLE_HDR : "white") + ";" +
                "-fx-text-fill: " + ("1".equals(pb.getText()) ? "white" : "#555") + ";" +
                "-fx-border-color: #dde2f0; -fx-border-radius: 4; -fx-background-radius: 4;" +
                "-fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 4 8;"
            );
            pageBox.getChildren().add(pb);
        }

        HBox bar = new HBox(10, showing, pSpacer, pageBox);
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    public static void main(String[] args) { launch(); }
}