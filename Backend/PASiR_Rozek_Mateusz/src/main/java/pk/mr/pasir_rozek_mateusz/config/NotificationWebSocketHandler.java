package pk.mr.pasir_rozek_mateusz.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import pk.mr.pasir_rozek_mateusz.security.JwtUtil;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    // Przechowuje aktywne sesje (Klucz: email użytkownika, Wartość: Sesja)
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper; // Do zamiany obiektu DTO na JSON

    public NotificationWebSocketHandler(JwtUtil jwtUtil, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        URI uri = session.getUri();
        if (uri != null && uri.getQuery() != null && uri.getQuery().contains("token=")) {
            String token = uri.getQuery().split("token=")[1];
            if (token.contains("&")) {
                token = token.split("&")[0];
            }

            if (jwtUtil.validateToken(token)) {
                String email = jwtUtil.extractUsername(token);
                sessions.put(email, session);
                System.out.println("Połączono WebSocket dla użytkownika: " + email);
            } else {
                session.close(CloseStatus.NOT_ACCEPTABLE);
            }
        } else {
            session.close(CloseStatus.NOT_ACCEPTABLE);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // Usuwanie sesji po rozłączeniu
        sessions.values().remove(session);
    }

    // Metoda, którą wywołasz ze swojego serwisu
    public void sendNotification(String userEmail, Object notification) {
        WebSocketSession session = sessions.get(userEmail);
        if (session != null && session.isOpen()) {
            try {
                String jsonMessage = objectMapper.writeValueAsString(notification);
                session.sendMessage(new TextMessage(jsonMessage));
            } catch (Exception e) {
                System.err.println("Błąd wysyłania powiadomienia: " + e.getMessage());
            }
        }
    }
}
