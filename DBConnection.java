import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    public static Connection getConnection() {
        Connection con = null;

        // 1. Fetch cloud database configurations safely from your environment variable setup
        // If no cloud variable is found, it automatically falls back to your local configurations.
        String dbUrl = System.getenv("SPRING_DATASOURCE_URL");
        String dbUser = System.getenv("SPRING_DATASOURCE_USERNAME");
        String dbPassword = System.getenv("SPRING_DATASOURCE_PASSWORD");

        // Local fallback defaults for testing sessions
        if (dbUrl == null || dbUrl.isEmpty()) {
            dbUrl = "jdbc:mysql://localhost:3306/hospital_db?useSSL=false&allowPublicKeyRetrieval=true";
        }
        if (dbUser == null || dbUser.isEmpty()) {
            dbUser = "root";
        }
        if (dbPassword == null || dbPassword.isEmpty()) {
            dbPassword = "oracle";
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            System.out.println("Database Connected Safely");
        } catch (Exception e) {
            System.out.println("Database Connection Failed: " + e.getMessage());
        }

        return con;
    }
}