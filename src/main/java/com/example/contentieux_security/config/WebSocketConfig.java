package com.example.contentieux_security.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.socket.config.annotation.*;

import java.security.Principal;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtDecoder jwtDecoder;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor; // ✅ branché

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(jwtHandshakeInterceptor) // ✅ handshake HTTP
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor == null
                        || !StompCommand.CONNECT.equals(accessor.getCommand())) {
                    return message;
                }

                log.info(">>> [STOMP CONNECT] Nouvelle tentative de connexion WebSocket");

                // ── Priorité 1 : username déjà résolu par JwtHandshakeInterceptor ──
                if (accessor.getSessionAttributes() != null) {
                    String usernameFromHandshake = (String)
                            accessor.getSessionAttributes().get("username");
                    if (usernameFromHandshake != null && !usernameFromHandshake.isBlank()) {
                        accessor.setUser(() -> usernameFromHandshake);
                        log.info("✅ [STOMP CONNECT] Utilisateur connecté (handshake): '{}'",
                                 usernameFromHandshake);
                        return message;
                    }
                }

                // ── Priorité 2 : header Authorization de la frame STOMP ──
                String authHeader = accessor.getFirstNativeHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    try {
                        Jwt jwt = jwtDecoder.decode(token);
                        String username = jwt.getClaimAsString("preferred_username");
                        if (username == null || username.isBlank()) {
                            username = jwt.getSubject();
                            log.warn("⚠️ [STOMP CONNECT] preferred_username absent — fallback sub");
                        }
                        final String finalUsername = username;
                        accessor.setUser(() -> finalUsername);
                        log.info("✅ [STOMP CONNECT] Utilisateur connecté (token STOMP): '{}'",
                                 finalUsername);
                    } catch (Exception e) {
                        log.error("❌ [STOMP CONNECT] Token invalide: {}", e.getMessage());
                    }
                } else {
                    log.error("❌ [STOMP CONNECT] Ni handshake username ni Authorization header");
                }

                return message;
            }
        });
    }
}