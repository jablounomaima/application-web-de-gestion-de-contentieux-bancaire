import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestDb2 {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3307/contentieux_db9?useSSL=false&serverTimezone=UTC";
        try (Connection con = DriverManager.getConnection(url, "root", "")) {
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT username, password FROM agent_bancaire");
            System.out.println("Agents:");
            while (rs.next()) {
                System.out.println(rs.getString("username") + ":" + rs.getString("password"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
