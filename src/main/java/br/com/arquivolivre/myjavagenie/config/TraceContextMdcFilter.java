package br.com.arquivolivre.myjavagenie.config;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter that populates MDC (Mapped Diagnostic Context) with OpenTelemetry trace information. This
 * enables log correlation by including trace_id and span_id in all log statements.
 */
@Component
@Order(1)
@ConditionalOnProperty(name = "opentelemetry.enabled", havingValue = "true", matchIfMissing = false)
public class TraceContextMdcFilter extends OncePerRequestFilter {

  private static final String TRACE_ID_KEY = "trace_id";
  private static final String SPAN_ID_KEY = "span_id";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    try {
      // Get current span context
      Span currentSpan = Span.current();
      SpanContext spanContext = currentSpan.getSpanContext();

      // Add trace and span IDs to MDC if available
      if (spanContext.isValid()) {
        MDC.put(TRACE_ID_KEY, spanContext.getTraceId());
        MDC.put(SPAN_ID_KEY, spanContext.getSpanId());
      }

      // Continue with the filter chain
      filterChain.doFilter(request, response);
    } finally {
      // Clean up MDC after request processing
      MDC.remove(TRACE_ID_KEY);
      MDC.remove(SPAN_ID_KEY);
    }
  }
}
