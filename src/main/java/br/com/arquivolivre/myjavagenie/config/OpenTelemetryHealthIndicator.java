package br.com.arquivolivre.myjavagenie.config;

import io.opentelemetry.api.OpenTelemetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;

/**
 * Health indicator for OpenTelemetry collector connectivity.
 * Checks if the OTLP endpoint is reachable.
 */
@Component
@ConditionalOnProperty(name = "opentelemetry.enabled", havingValue = "true", matchIfMissing = false)
public class OpenTelemetryHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(OpenTelemetryHealthIndicator.class);

    private final OpenTelemetryConfig.OpenTelemetryProperties properties;
    private final OpenTelemetry openTelemetry;

    public OpenTelemetryHealthIndicator(OpenTelemetryConfig.OpenTelemetryProperties properties,
                                        OpenTelemetry openTelemetry) {
        this.properties = properties;
        this.openTelemetry = openTelemetry;
    }

    @Override
    public Health health() {
        try {
            // Check if OpenTelemetry is initialized
            if (openTelemetry == null) {
                return Health.down()
                        .withDetail("status", "OpenTelemetry not initialized")
                        .build();
            }

            // Check collector connectivity
            boolean collectorReachable = checkCollectorConnectivity();

            if (collectorReachable) {
                return Health.up()
                        .withDetail("status", "OpenTelemetry collector is reachable")
                        .withDetail("endpoint", properties.getTraces().getEndpoint())
                        .withDetail("service", properties.getServiceName())
                        .withDetail("traces_enabled", properties.getTraces().isEnabled())
                        .withDetail("metrics_enabled", properties.getMetrics().isEnabled())
                        .withDetail("logs_enabled", properties.getLogs().isEnabled())
                        .build();
            } else {
                return Health.down()
                        .withDetail("status", "OpenTelemetry collector is not reachable")
                        .withDetail("endpoint", properties.getTraces().getEndpoint())
                        .withDetail("note", "Application continues to function, but telemetry data may not be exported")
                        .build();
            }
        } catch (Exception e) {
            logger.error("Error checking OpenTelemetry health", e);
            return Health.down()
                    .withDetail("status", "Error checking OpenTelemetry health")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }

    /**
     * Checks if the OpenTelemetry collector is reachable.
     */
    private boolean checkCollectorConnectivity() {
        try {
            String endpoint = properties.getTraces().getEndpoint();
            URI uri = URI.create(endpoint);

            String host = uri.getHost();
            int port = uri.getPort();

            // Default OTLP gRPC port
            if (port == -1) {
                port = 4317;
            }

            // Try to establish a socket connection
            try (Socket socket = new Socket(host, port)) {
                return socket.isConnected();
            }
        } catch (IOException e) {
            logger.debug("OpenTelemetry collector not reachable: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            logger.warn("Error checking OpenTelemetry collector connectivity: {}", e.getMessage());
            return false;
        }
    }
}
