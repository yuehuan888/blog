package com.blog.websocket;

import com.blog.entity.Danmaku;
import com.blog.mapper.DanmakuMapper;
import com.blog.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for real-time danmaku.
 * One room per video (articleId). Broadcasts new danmaku to all viewers.
 */
@Component
public class DanmakuWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(DanmakuWebSocketHandler.class);

    /** articleId → set of sessions */
    private final ConcurrentHashMap<Long, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private DanmakuMapper danmakuMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long articleId = getArticleId(session);
        if (articleId == null) {
            try { session.close(); } catch (IOException ignored) {}
            return;
        }

        rooms.computeIfAbsent(articleId, k -> ConcurrentHashMap.newKeySet()).add(session);
        log.info("WS connected: articleId={}, total={}", articleId, rooms.get(articleId).size());

        // Send history
        try {
            List<Danmaku> history = danmakuMapper.selectByArticleId(articleId);
            String msg = objectMapper.writeValueAsString(Map.of(
                    "type", "history",
                    "danmakuList", history
            ));
            session.sendMessage(new TextMessage(msg));
        } catch (Exception e) {
            log.warn("Failed to send danmaku history for article={}", articleId, e);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Long articleId = getArticleId(session);
        if (articleId == null) return;

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> msg = objectMapper.readValue(message.getPayload(), Map.class);
            String type = (String) msg.get("type");

            if ("join".equals(type)) {
                // Already handled in afterConnectionEstablished
            } else if ("send".equals(type)) {
                String content = (String) msg.get("content");
                String color = (String) msg.getOrDefault("color", "#FFFFFF");
                String mode = (String) msg.getOrDefault("mode", "scroll");
                double timestampSec = ((Number) msg.get("timestampSec")).doubleValue();

                // Extract userId from session (JWT auth)
                Long userId = getUserId(session);
                String nickname = getNickname(session);
                if (userId == null) return;

                // Persist to DB
                Danmaku danmaku = new Danmaku();
                danmaku.setArticleId(articleId);
                danmaku.setUserId(userId);
                danmaku.setContent(content);
                danmaku.setTimestampSec(timestampSec);
                danmaku.setColor(color);
                danmaku.setMode(mode);
                danmakuMapper.insert(danmaku);

                // Broadcast to all viewers in the room
                Map<String, Object> broadcast = Map.of(
                        "type", "new",
                        "id", danmaku.getId(),
                        "userId", userId,
                        "nickname", nickname != null ? nickname : "User" + userId,
                        "content", content,
                        "color", color,
                        "mode", mode,
                        "timestampSec", timestampSec
                );
                String broadcastJson = objectMapper.writeValueAsString(broadcast);

                Set<WebSocketSession> sessions = rooms.get(articleId);
                if (sessions != null) {
                    for (WebSocketSession s : sessions) {
                        if (s.isOpen()) {
                            try {
                                s.sendMessage(new TextMessage(broadcastJson));
                            } catch (IOException e) {
                                log.warn("Failed to send danmaku to session", e);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Danmaku WS error for articleId={}", articleId, e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long articleId = getArticleId(session);
        if (articleId != null) {
            Set<WebSocketSession> sessions = rooms.get(articleId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    rooms.remove(articleId);
                }
            }
            log.info("WS disconnected: articleId={}, remaining={}", articleId,
                    sessions != null ? sessions.size() : 0);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WS transport error: articleId={}", getArticleId(session), exception);
    }

    private Long getArticleId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) return null;
        String path = uri.getPath();
        // Path: /ws/video/{id}/danmaku
        String[] parts = path.split("/");
        try {
            return Long.parseLong(parts[3]);
        } catch (Exception e) {
            return null;
        }
    }

    private Long getUserId(WebSocketSession session) {
        String token = (String) session.getAttributes().get("token");
        if (token == null) {
            // Try query param
            URI uri = session.getUri();
            if (uri != null && uri.getQuery() != null) {
                for (String param : uri.getQuery().split("&")) {
                    String[] kv = param.split("=");
                    if (kv.length == 2 && "token".equals(kv[0])) {
                        token = kv[1];
                    }
                }
            }
        }
        if (token != null) {
            try {
                return jwtUtil.getUserIdFromToken(token);
            } catch (Exception e) {
                log.warn("Invalid WS token", e);
            }
        }
        return null;
    }

    private String getNickname(WebSocketSession session) {
        String token = (String) session.getAttributes().get("token");
        if (token == null) {
            URI uri = session.getUri();
            if (uri != null && uri.getQuery() != null) {
                for (String param : uri.getQuery().split("&")) {
                    String[] kv = param.split("=");
                    if (kv.length == 2 && "token".equals(kv[0])) {
                        token = kv[1];
                    }
                }
            }
        }
        if (token != null) {
            try {
                return jwtUtil.getNicknameFromToken(token);
            } catch (Exception e) {
                // ignore
            }
        }
        return null;
    }
}
