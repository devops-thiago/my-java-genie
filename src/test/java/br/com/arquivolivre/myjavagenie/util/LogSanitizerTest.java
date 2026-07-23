package br.com.arquivolivre.myjavagenie.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Unit tests for {@link LogSanitizer}. */
class LogSanitizerTest {

  @Test
  void sanitizeStringReplacesCarriageReturnAndLineFeed() {
    assertThat(LogSanitizer.sanitize("line1\r\nline2")).isEqualTo("line1__line2");
    assertThat(LogSanitizer.sanitize("only\nnewline")).isEqualTo("only_newline");
    assertThat(LogSanitizer.sanitize("only\rreturn")).isEqualTo("only_return");
  }

  @Test
  void sanitizeStringLeavesCleanValuesUnchanged() {
    assertThat(LogSanitizer.sanitize("clean value 123")).isEqualTo("clean value 123");
  }

  @Test
  void sanitizeStringReturnsNullForNull() {
    assertThat(LogSanitizer.sanitize((String) null)).isNull();
  }

  @Test
  void sanitizeObjectUsesStringRepresentation() {
    assertThat(LogSanitizer.sanitize((Object) 42)).isEqualTo("42");
    assertThat(LogSanitizer.sanitize((Object) "a\r\nb")).isEqualTo("a__b");
  }

  @Test
  void sanitizeObjectReturnsNullForNull() {
    assertThat(LogSanitizer.sanitize((Object) null)).isNull();
  }

  @Test
  void sanitizeObjectSanitizesToStringOutput() {
    Object withNewlineToString =
        new Object() {
          @Override
          public String toString() {
            return "forged\nentry";
          }
        };
    assertThat(LogSanitizer.sanitize(withNewlineToString)).isEqualTo("forged_entry");
  }
}
