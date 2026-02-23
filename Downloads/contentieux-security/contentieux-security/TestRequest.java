import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class TestRequest {
    public static void main(String[] args) throws Exception {
        // Authenticate first
        String loginJson = "{\"username\": \"agent1\", \"password\": \"1234\"}";
        URL loginUrl = new URL("http://localhost:8097/api/auth/login");
        HttpURLConnection loginCon = (HttpURLConnection) loginUrl.openConnection();
        loginCon.setRequestMethod("POST");
        loginCon.setRequestProperty("Content-Type", "application/json");
        loginCon.setDoOutput(true);
        try (OutputStream os = loginCon.getOutputStream()) {
            os.write(loginJson.getBytes(StandardCharsets.UTF_8));
        }

        if (loginCon.getResponseCode() != 200) {
            System.out.println("Login Failed: " + loginCon.getResponseCode());
            // It could be that agent1 doesn't exist. Let's still try to fetch without token
            // and see behavior
        } else {
            System.out.println("Login Successful");
        }

        // Let's test the endpoint directly using an OPTIONS request or something just
        // to see if it exists
        URL targetUrl = new URL("http://localhost:8097/api/utilisateurs");
        HttpURLConnection con = (HttpURLConnection) targetUrl.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setDoOutput(true);
        String payload = "{\"username\": \"testuser\", \"nom\": \"Test\", \"prenom\": \"User\", \"email\": \"test@example.com\", \"telephone\": \"1234\", \"password\": \"1234\", \"role\": \"AVOCAT\"}";
        try (OutputStream os = con.getOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }

        System.out.println("Response Code: " + con.getResponseCode());
        InputStream is = con.getResponseCode() >= 400 ? con.getErrorStream() : con.getInputStream();
        if (is != null) {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        }
    }
}
