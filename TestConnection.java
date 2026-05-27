import java.sql.Connection;

public class TestConnection {

    public static void main(String[] args) {
        System.out.println("Initiating database connection diagnostic test...");
        
        // Check what target environment configuration is currently active
        String cloudUrl = System.getenv("SPRING_DATASOURCE_URL");
        if (cloudUrl != null && !cloudUrl.isEmpty()) {
            System.out.println("Target Profile: CLOUD (Active Environment Variable Detected)");
        } else {
            System.out.println("Target Profile: LOCAL (Falling back to localhost:3306)");
        }

        // Attempt the actual handshake connection
        Connection con = DBConnection.getConnection();

        System.out.println("----------------------------------------");
        if (con != null) {
            System.out.println(">>> SUCCESS: System verified connection to database!");
            try {
                con.close(); // Clean up resource after testing
            } catch (Exception ignored) {}
        } else {
            System.out.println(">>> ERROR: Connection failed! Please check credentials or network parameters.");
        }
        System.out.println("----------------------------------------");
    }
}