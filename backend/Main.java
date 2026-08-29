import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main (String args[]) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/shortenUrl", new ShortenerHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Server started on port 8080");
    }

    static class ShortenerHandler implements HttpHandler {
        @Override
        @SuppressWarnings("ConvertToTryWithResources")
        public void handle(HttpExchange exchange) throws IOException {

            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
                exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

                exchange.sendResponseHeaders(204, -1);
                return;
            }

            
            InputStream input = exchange.getRequestBody();
            String longUrl = new String(input.readAllBytes()); 
            System.out.println("Received: " + longUrl);
            
            
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            // String shortUrl = generateShortUrlJson(longUrl);
            
            // exchange.getResponseHeaders().add(
            //     "Access-Control-Allow-Origin",
            //     "*"
            // );
            // exchange.sendResponseHeaders(
            //     200, shortUrl.getBytes().length
            // );

            // OutputStream output = exchange.getResponseBody();
            // output.write(shortUrl.getBytes());
            // output.close();

            String shortUrl = generateShortUrlJson(longUrl);

            String json = "{ \"shortUrl\": \"" + shortUrl + "\" }";

            exchange.getResponseHeaders().set(
                "Content-Type",
                "application/json; charset=UTF-8"
            );

            byte[] response = json.getBytes(StandardCharsets.UTF_8);

            exchange.sendResponseHeaders(200, response.length);

            try (OutputStream output = exchange.getResponseBody()) {
                output.write(response);
            }
        }

        private void dataBaseRun () {
            String url = "jdbc:mysql://localhost:3306/mydatabase";
            String user = "root";
            String password = "password";

            try (Connection conn = DriverManager.getConnection(url, user, password);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT id, name FROM users")) {
                
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    System.out.println("ID: " + id + ", Name: " + name);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private String generateShortUrlJson (String longUrl) {
            String primaryKey = new String("14"); 
            return new String("srt." + primaryKey);  
        }
    }
}