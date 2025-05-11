package HTTPhandler;

import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dao.ChatMemberDao;
import entity.Chat;
import entity.ChatMember;
import entity.User;
import entity.ChatRole;
import util.HibernateUtil;
import util.JwtUtil;

import org.hibernate.Session;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;

public class ChatMemberHttpHandler implements HttpHandler {

    private final ChatMemberDao chatMemberDao = new ChatMemberDao();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth == null) {
            exchange.sendResponseHeaders(401, -1);
            return;
        }
        String callerUsername = JwtUtil.validateToken(auth);
        if (callerUsername == null) {
            exchange.sendResponseHeaders(401, -1);
            return;
        }

        InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
        JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

        String memberUsername = json.get("username").getAsString();
        Long chatId = json.get("chatId").getAsLong();
        String roleStr = json.get("role").getAsString();

        ChatRole role;
        try {
            role = ChatRole.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, 400, "Invalid role: " + roleStr);
            return;
        }

        User member = chatMemberDao.findUserByUsername(memberUsername);
        Chat chat;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            chat = session.get(Chat.class, chatId);
        }

        if (member == null || chat == null) {
            sendResponse(exchange, 400, "Unknown user or chat");
            return;
        }

        if (chatMemberDao.chatMemberExists(chatId, member.getId())) {
            sendResponse(exchange, 409, "Chat member already exists");
            return;
        }

        ChatMember cm = new ChatMember();
        cm.setUser(member);
        cm.setChat(chat);
        cm.setJoinedAt(new Timestamp(System.currentTimeMillis()));
        cm.setRole(role);

        try {
            chatMemberDao.save(cm);
            sendResponse(exchange, 201, "ChatMember created successfully");
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "Failed to create ChatMember");
        }
    }

    private void sendResponse(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
