package br.com.arquivolivre.myjavagenie.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Filter for logging HTTP requests and responses. Captures request details, response status, and
 * processing time.
 */
@Component
public class RequestResponseLoggingFilter implements Filter {

  private static final Logger logger = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);
  private static final int MAX_PAYLOAD_LENGTH = 1000;

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    if (request instanceof HttpServletRequest httpRequest
        && response instanceof HttpServletResponse httpResponse) {

      // Wrap request and response to cache content
      ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(httpRequest);
      ContentCachingResponseWrapper responseWrapper =
          new ContentCachingResponseWrapper(httpResponse);

      long startTime = System.currentTimeMillis();

      try {
        // Log request
        logRequest(requestWrapper);

        // Continue with the filter chain
        chain.doFilter(requestWrapper, responseWrapper);

      } finally {
        long duration = System.currentTimeMillis() - startTime;

        // Log response
        logResponse(responseWrapper, duration);

        // Copy response content back to original response
        responseWrapper.copyBodyToResponse();
      }
    } else {
      chain.doFilter(request, response);
    }
  }

  /** Logs HTTP request details. */
  private void logRequest(ContentCachingRequestWrapper request) {
    StringBuilder logMessage = new StringBuilder();
    logMessage.append("HTTP Request: ");
    logMessage.append(request.getMethod()).append(" ");
    logMessage.append(request.getRequestURI());

    String queryString = request.getQueryString();
    if (queryString != null) {
      logMessage.append("?").append(queryString);
    }

    // Log headers (excluding sensitive ones)
    logMessage.append(" | Headers: {");
    Enumeration<String> headerNames = request.getHeaderNames();
    boolean first = true;
    while (headerNames.hasMoreElements()) {
      String headerName = headerNames.nextElement();
      if (!isSensitiveHeader(headerName)) {
        if (!first) {
          logMessage.append(", ");
        }
        logMessage.append(headerName).append("=").append(request.getHeader(headerName));
        first = false;
      }
    }
    logMessage.append("}");

    // Log request body for POST/PUT requests
    if ("POST".equals(request.getMethod()) || "PUT".equals(request.getMethod())) {
      String payload = getRequestPayload(request);
      if (payload != null && !payload.isEmpty()) {
        logMessage.append(" | Body: ").append(truncate(payload));
      }
    }

    logger.info(logMessage.toString());
  }

  /** Logs HTTP response details. */
  private void logResponse(ContentCachingResponseWrapper response, long duration) {
    StringBuilder logMessage = new StringBuilder();
    logMessage.append("HTTP Response: ");
    logMessage.append("Status=").append(response.getStatus());
    logMessage.append(" | Duration=").append(duration).append("ms");

    // Log response body for non-binary content
    String contentType = response.getContentType();
    if (contentType != null && isLoggableContentType(contentType)) {
      String payload = getResponsePayload(response);
      if (payload != null && !payload.isEmpty()) {
        logMessage.append(" | Body: ").append(truncate(payload));
      }
    }

    logger.info(logMessage.toString());
  }

  /** Extracts request payload from cached content. */
  private String getRequestPayload(ContentCachingRequestWrapper request) {
    byte[] content = request.getContentAsByteArray();
    if (content.length > 0) {
      return new String(content, StandardCharsets.UTF_8);
    }
    return null;
  }

  /** Extracts response payload from cached content. */
  private String getResponsePayload(ContentCachingResponseWrapper response) {
    byte[] content = response.getContentAsByteArray();
    if (content.length > 0) {
      return new String(content, StandardCharsets.UTF_8);
    }
    return null;
  }

  /** Checks if a header is sensitive and should not be logged. */
  private boolean isSensitiveHeader(String headerName) {
    String lowerName = headerName.toLowerCase();
    return lowerName.contains("authorization")
        || lowerName.contains("password")
        || lowerName.contains("token")
        || lowerName.contains("api-key")
        || lowerName.contains("secret");
  }

  /** Checks if content type should be logged. */
  private boolean isLoggableContentType(String contentType) {
    return contentType.contains("json")
        || contentType.contains("xml")
        || contentType.contains("text");
  }

  /** Truncates a string to maximum length for logging. */
  private String truncate(String str) {
    if (str == null) {
      return "";
    }
    if (str.length() <= MAX_PAYLOAD_LENGTH) {
      return str;
    }
    return str.substring(0, MAX_PAYLOAD_LENGTH) + "... (truncated)";
  }
}
