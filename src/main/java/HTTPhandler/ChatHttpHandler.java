package HTTPhandler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dao.ChatDao;
import entity.Chat;
import util.JwtUtil;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatHttpHandler implements HttpHandler {
    private final ChatDao chatDao = new ChatDao();
    private final Gson gson = new Gson();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth == null) {
            exchange.sendResponseHeaders(401, -1);
            return;
        }
        String username = JwtUtil.validateToken(auth);
        if (username == null) {
            exchange.sendResponseHeaders(401, -1);
            return;
        }

        String method = exchange.getRequestMethod();
        try {
            if ("POST".equalsIgnoreCase(method)) {
                handleCreate(exchange, username);
            } else if ("GET".equalsIgnoreCase(method)) {
                handleList(exchange, username);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            exchange.sendResponseHeaders(500, -1);
        }
    }

    private void handleCreate(HttpExchange exchange, String username) throws IOException {
        // Parse Chat from body
        InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
        Chat chat = gson.fromJson(reader, Chat.class);

        chat.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        chatDao.saveChat(chat);

        Map<String, Object> result = new HashMap<>();
        result.put("id", chat.getId());
        String json = gson.toJson(result);
        byte[] resp = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(201, resp.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(resp);
        }
    }

    private void handleList(HttpExchange exchange, String username) throws IOException {
        List<Chat> chats = chatDao.getChatsForUser(username);

        String json = gson.toJson(chats);
        byte[] resp = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, resp.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(resp);
        }
    }
}
