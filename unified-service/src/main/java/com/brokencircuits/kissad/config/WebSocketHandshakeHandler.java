package com.brokencircuits.kissad.config;

import com.sun.security.auth.UserPrincipal;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

/**
 * Assigns a unique Principal to each anonymous WebSocket session.
 */
class WebSocketHandshakeHandler extends DefaultHandshakeHandler {
  @Override
  protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
    // Assign a unique, random name as the Principal for each session
    return new UserPrincipal(UUID.randomUUID().toString());
  }
}
