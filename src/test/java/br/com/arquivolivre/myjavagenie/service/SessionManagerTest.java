package br.com.arquivolivre.myjavagenie.service;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.arquivolivre.myjavagenie.model.ChatSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SessionManagerTest {

  private SessionManager sessionManager;

  @BeforeEach
  void setUp() {
    sessionManager = new SessionManager();
    ReflectionTestUtils.setField(sessionManager, "sessionTimeoutSeconds", 1800L);
  }

  @Test
  void getOrCreateSessionShouldCreateWhenIdMissing() {
    ChatSession session = sessionManager.getOrCreateSession(null);
    assertThat(session.getSessionId()).isNotBlank();
    assertThat(sessionManager.getSession(session.getSessionId())).isSameAs(session);
  }

  @Test
  void getOrCreateSessionShouldReuseExistingId() {
    ChatSession first = sessionManager.getOrCreateSession("abc");
    ChatSession second = sessionManager.getOrCreateSession("abc");
    assertThat(second).isSameAs(first);
  }
}
