package com.example.contentieux_security.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtDecoder jwtDecoder;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {

        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            log.error("❌ [WS] Request non-servlet");
            return false;
        }

        // ── 1. Query param ?token= (SockJS) ──
        String token = servletRequest.getServletRequest().getParameter("token");

        // ── 2. Fallback header Authorization ──
        if (token == null || token.isBlank()) {
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
                log.debug("🔄 [WS] Token lu depuis header Authorization");
            }
        }

        if (token == null || token.isBlank()) {
            log.error("❌ [WS] Aucun token fourni");
            return false;
        }

        if (token.split("\\.").length != 3) {
            log.error("❌ [WS] Token malformé: '{}'", token);
            return false;
        }

        try {
            Jwt jwt = jwtDecoder.decode(token);
            String username = jwt.getClaimAsString("preferred_username");
            if (username == null) username = jwt.getSubject();

            attributes.put("username", username); // ✅ récupéré par le ChannelInterceptor
            log.info("✅ [WS] Handshake OK — username='{}'", username);
            return true;

        } catch (Exception e) {
            log.error("❌ [WS] Token invalide: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest req, ServerHttpResponse res,
                               WebSocketHandler handler, Exception ex) {}
}