package br.com.arquivolivre.myjavagenie.config;

import br.com.arquivolivre.myjavagenie.websocket.ChatWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

  private final ChatWebSocketHandler chatWebSocketHandler;

  public WebSocketConfig(ChatWebSocketHandler chatWebSocketHandler) {
    this.chatWebSocketHandler = chatWebSocketHandler;
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(chatWebSocketHandler, "/ws/chat").setAllowedOriginPatterns("*");
  }
}
