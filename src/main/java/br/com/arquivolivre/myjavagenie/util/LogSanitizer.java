package br.com.arquivolivre.myjavagenie.util;

/**
 * Utility for neutralizing CR/LF sequences in values before they are written to application logs.
 *
 * <p>Untrusted values (request parameters, exception messages derived from user input, etc.) can
 * contain carriage-return/line-feed characters that would otherwise let an attacker forge or split
 * log records (log injection). Passing such values through {@link #sanitize(Object)} replaces those
 * control characters so each logged value stays on a single line.
 */
public final class LogSanitizer {

  private LogSanitizer() {}

  /**
   * Returns a single-line representation of the given value with CR/LF characters replaced.
   *
   * @param value the value to sanitize; may be {@code null}
   * @return the sanitized string, or {@code null} if {@code value} is {@code null}
   */
  public static String sanitize(Object value) {
    if (value == null) {
      return null;
    }
    return sanitize(String.valueOf(value));
  }

  /**
   * Returns the given string with CR/LF characters replaced so it cannot span multiple log lines.
   *
   * @param value the string to sanitize; may be {@code null}
   * @return the sanitized string, or {@code null} if {@code value} is {@code null}
   */
  public static String sanitize(String value) {
    if (value == null) {
      return null;
    }
    return value.replace('\r', '_').replace('\n', '_');
  }
}
