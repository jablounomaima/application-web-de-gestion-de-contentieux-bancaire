import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestDb {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3307/contentieux_db9?useSSL=false&serverTimezone=UTC";
        try (Connection con = DriverManager.getConnection(url, "root", "")) {
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SHOW TABLES");
            System.out.println("Tables:");
            while (rs.next()) {
                System.out.println(rs.getString(1));
            }
            // Check if utilisateur table exists
            ResultSet rs2 = stmt.executeQuery("SELECT * FROM utilisateur");
            System.out.println("Utilisateur table accessed successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
