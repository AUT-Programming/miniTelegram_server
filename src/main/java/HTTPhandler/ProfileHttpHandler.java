package HTTPhandler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import entity.User;
import entity.Profile;
import org.hibernate.Session;
import org.hibernate.Transaction;
import util.HibernateUtil;
import util.JwtUtil;
import dto.*;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ProfileHttpHandler implements HttpHandler {

    private void handleUpdateProfile(HttpExchange exchange, String username) throws IOException {
        InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
        Profile updatedProfile = new Gson().fromJson(reader, Profile.class);

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();

            User user = getUserByUsername(session, username);
            if (user == null) {
                exchange.sendResponseHeaders(404, -1); // Not Found
                return;
            }

            Profile profile = user.getProfile();
            if (profile == null) {
                profile = new Profile();
                profile.setUser(user);
            }

            profile.setDisplayName(updatedProfile.getDisplayName());
            profile.setProfilePicture(updatedProfile.getProfilePicture());

            session.saveOrUpdate(profile);
            tx.commit();

            String response = "Profile updated successfully";
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
            exchange.sendResponseHeaders(500, -1); // Internal Server Error
        }
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String token = exchange.getRequestHeaders().getFirst("Authorization");

        if (token == null || token.isEmpty()) {
            exchange.sendResponseHeaders(401, -1); // Unauthorized
            return;
        }

        String username = JwtUtil.validateToken(token);
        if (username == null) {
            exchange.sendResponseHeaders(401, -1); // Unauthorized
            return;
        }

        if ("GET".equals(exchange.getRequestMethod())) {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/profile")) {
                handleGetProfile(exchange, username);
            } else {
                exchange.sendResponseHeaders(404, -1); // Not Found
            }
        } else if ("PUT".equals(exchange.getRequestMethod())) {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/profile")) {
                handleUpdateProfile(exchange, username);
            } else {
                exchange.sendResponseHeaders(404, -1); // Not Found
            }
        } else {
            exchange.sendResponseHeaders(405, -1); // Method Not Allowed
        }
    }

    private void handleGetProfile(HttpExchange exchange, String username) throws IOException {
        // Fetch user and profile
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            User user = getUserByUsername(session, username);
            if (user == null || user.getProfile() == null) {
                exchange.sendResponseHeaders(404, -1); // Not Found
                return;
            }

            Profile profile = user.getProfile();

            // Use DTO to avoid full serialization
            ProfileDTO profileDTO = new ProfileDTO(
                    profile.getUser().getUsername(),
                    profile.getDisplayName(),
                    profile.getProfilePicture()
            );

            String response = new Gson().toJson(profileDTO);

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }

        } catch (Exception e) {
            e.printStackTrace();
            exchange.sendResponseHeaders(500, -1); // Internal Server Error
        }
    }


    private User getUserByUsername(Session session, String username) {
        return session.createQuery("from User where username = :username", User.class)
                .setParameter("username", username)
                .uniqueResult();
    }
}
