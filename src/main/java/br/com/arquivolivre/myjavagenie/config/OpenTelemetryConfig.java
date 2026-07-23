package br.com.arquivolivre.myjavagenie.config;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.ResourceAttributes;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * Configuration class for OpenTelemetry observability. Sets up traces, metrics, and logs exporters
 * with OTLP protocol.
 */
@Configuration
@ConditionalOnProperty(name = "opentelemetry.enabled", havingValue = "true", matchIfMissing = false)
public class OpenTelemetryConfig {

  private static final Logger logger = LoggerFactory.getLogger(OpenTelemetryConfig.class);

  private final OpenTelemetryProperties properties;

  public OpenTelemetryConfig(OpenTelemetryProperties properties) {
    this.properties = properties;
    logger.info("Initializing OpenTelemetry with service name: {}", properties.getServiceName());
  }

  /** Creates the OpenTelemetry SDK instance with configured exporters. */
  @Bean
  public OpenTelemetry openTelemetry() {
    Resource resource =
        Resource.getDefault()
            .merge(
                Resource.create(
                    Attributes.builder()
                        .put(ResourceAttributes.SERVICE_NAME, properties.getServiceName())
                        .put(ResourceAttributes.SERVICE_VERSION, properties.getServiceVersion())
                        .put(ResourceAttributes.DEPLOYMENT_ENVIRONMENT, properties.getEnvironment())
                        .build()));

    var sdkBuilder = OpenTelemetrySdk.builder();

    // Configure Tracer Provider
    if (properties.getTraces().isEnabled()) {
      SdkTracerProvider tracerProvider = configurTracerProvider(resource);
      sdkBuilder.setTracerProvider(tracerProvider);
      logger.info(
          "OpenTelemetry traces enabled with endpoint: {}", properties.getTraces().getEndpoint());
    }

    // Configure Meter Provider
    if (properties.getMetrics().isEnabled()) {
      SdkMeterProvider meterProvider = configureMeterProvider(resource);
      sdkBuilder.setMeterProvider(meterProvider);
      logger.info(
          "OpenTelemetry metrics enabled with endpoint: {}", properties.getMetrics().getEndpoint());
    }

    // Configure Logger Provider
    if (properties.getLogs().isEnabled()) {
      SdkLoggerProvider loggerProvider = configureLoggerProvider(resource);
      sdkBuilder.setLoggerProvider(loggerProvider);
      logger.info(
          "OpenTelemetry logs enabled with endpoint: {}", properties.getLogs().getEndpoint());
    }

    // Build a local SDK bean. Register as global only when nothing else (e.g. the
    // OpenTelemetry Spring starter) has already called GlobalOpenTelemetry.set —
    // otherwise Spring tests fail with "GlobalOpenTelemetry.set has already been called".
    OpenTelemetrySdk openTelemetry = sdkBuilder.setPropagators(ContextPropagators.noop()).build();
    try {
      GlobalOpenTelemetry.set(openTelemetry);
    } catch (IllegalStateException alreadyRegistered) {
      logger.warn(
          "Global OpenTelemetry already registered; using SDK as Spring bean only: {}",
          alreadyRegistered.getMessage());
    }

    logger.info("OpenTelemetry SDK initialized successfully");
    return openTelemetry;
  }

  /** Configures the tracer provider with OTLP exporter. */
  private SdkTracerProvider configurTracerProvider(Resource resource) {
    try {
      OtlpGrpcSpanExporter spanExporter =
          OtlpGrpcSpanExporter.builder()
              .setEndpoint(properties.getTraces().getEndpoint())
              .setTimeout(10, TimeUnit.SECONDS)
              .build();

      return SdkTracerProvider.builder()
          .setResource(resource)
          .addSpanProcessor(
              BatchSpanProcessor.builder(spanExporter)
                  .setScheduleDelay(Duration.ofSeconds(5))
                  .build())
          .setSampler(Sampler.traceIdRatioBased(properties.getTraces().getSamplingRate()))
          .build();
    } catch (Exception e) {
      logger.error(
          "Failed to configure tracer provider, traces will not be exported: {}", e.getMessage());
      // Return a no-op tracer provider to allow application to continue
      return SdkTracerProvider.builder()
          .setResource(resource)
          .setSampler(Sampler.alwaysOff())
          .build();
    }
  }

  /** Configures the meter provider with OTLP exporter. */
  private SdkMeterProvider configureMeterProvider(Resource resource) {
    try {
      OtlpGrpcMetricExporter metricExporter =
          OtlpGrpcMetricExporter.builder()
              .setEndpoint(properties.getMetrics().getEndpoint())
              .setTimeout(10, TimeUnit.SECONDS)
              .build();

      return SdkMeterProvider.builder()
          .setResource(resource)
          .registerMetricReader(
              PeriodicMetricReader.builder(metricExporter)
                  .setInterval(Duration.ofMillis(properties.getMetrics().getExportIntervalMillis()))
                  .build())
          .build();
    } catch (Exception e) {
      logger.error(
          "Failed to configure meter provider, metrics will not be exported: {}", e.getMessage());
      // Return a no-op meter provider to allow application to continue
      return SdkMeterProvider.builder().setResource(resource).build();
    }
  }

  /** Configures the logger provider with OTLP exporter. */
  private SdkLoggerProvider configureLoggerProvider(Resource resource) {
    try {
      OtlpGrpcLogRecordExporter logExporter =
          OtlpGrpcLogRecordExporter.builder()
              .setEndpoint(properties.getLogs().getEndpoint())
              .setTimeout(10, TimeUnit.SECONDS)
              .build();

      return SdkLoggerProvider.builder()
          .setResource(resource)
          .addLogRecordProcessor(
              BatchLogRecordProcessor.builder(logExporter)
                  .setScheduleDelay(Duration.ofSeconds(5))
                  .build())
          .build();
    } catch (Exception e) {
      logger.error(
          "Failed to configure logger provider, logs will not be exported: {}", e.getMessage());
      // Return a no-op logger provider to allow application to continue
      return SdkLoggerProvider.builder().setResource(resource).build();
    }
  }

  /** Creates a Tracer bean for manual instrumentation. */
  @Bean
  public Tracer tracer(OpenTelemetry openTelemetry) {
    return openTelemetry.getTracer(properties.getServiceName());
  }

  /** Creates a Meter bean for custom metrics. */
  @Bean
  public Meter meter(OpenTelemetry openTelemetry) {
    return openTelemetry.getMeter(properties.getServiceName());
  }

  /** Configuration properties for OpenTelemetry. */
  @Component
  @ConfigurationProperties(prefix = "opentelemetry")
  public static class OpenTelemetryProperties {
    private boolean enabled = true;
    private String serviceName = "java-rag-system";
    private String serviceVersion = "1.0.0";
    private String environment = "development";
    private TracesConfig traces = new TracesConfig();
    private MetricsConfig metrics = new MetricsConfig();
    private LogsConfig logs = new LogsConfig();

    // Getters and setters
    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getServiceName() {
      return serviceName;
    }

    public void setServiceName(String serviceName) {
      this.serviceName = serviceName;
    }

    public String getServiceVersion() {
      return serviceVersion;
    }

    public void setServiceVersion(String serviceVersion) {
      this.serviceVersion = serviceVersion;
    }

    public String getEnvironment() {
      return environment;
    }

    public void setEnvironment(String environment) {
      this.environment = environment;
    }

    public TracesConfig getTraces() {
      return traces;
    }

    public void setTraces(TracesConfig traces) {
      this.traces = traces;
    }

    public MetricsConfig getMetrics() {
      return metrics;
    }

    public void setMetrics(MetricsConfig metrics) {
      this.metrics = metrics;
    }

    public LogsConfig getLogs() {
      return logs;
    }

    public void setLogs(LogsConfig logs) {
      this.logs = logs;
    }

    public static class TracesConfig {
      private boolean enabled = true;
      private String exporter = "otlp";
      private String endpoint = "http://localhost:4317";
      private double samplingRate = 1.0;

      public boolean isEnabled() {
        return enabled;
      }

      public void setEnabled(boolean enabled) {
        this.enabled = enabled;
      }

      public String getExporter() {
        return exporter;
      }

      public void setExporter(String exporter) {
        this.exporter = exporter;
      }

      public String getEndpoint() {
        return endpoint;
      }

      public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
      }

      public double getSamplingRate() {
        return samplingRate;
      }

      public void setSamplingRate(double samplingRate) {
        this.samplingRate = samplingRate;
      }
    }

    public static class MetricsConfig {
      private boolean enabled = true;
      private String exporter = "otlp";
      private String endpoint = "http://localhost:4317";
      private long exportIntervalMillis = 60000;

      public boolean isEnabled() {
        return enabled;
      }

      public void setEnabled(boolean enabled) {
        this.enabled = enabled;
      }

      public String getExporter() {
        return exporter;
      }

      public void setExporter(String exporter) {
        this.exporter = exporter;
      }

      public String getEndpoint() {
        return endpoint;
      }

      public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
      }

      public long getExportIntervalMillis() {
        return exportIntervalMillis;
      }

      public void setExportIntervalMillis(long exportIntervalMillis) {
        this.exportIntervalMillis = exportIntervalMillis;
      }
    }

    public static class LogsConfig {
      private boolean enabled = true;
      private String exporter = "otlp";
      private String endpoint = "http://localhost:4317";

      public boolean isEnabled() {
        return enabled;
      }

      public void setEnabled(boolean enabled) {
        this.enabled = enabled;
      }

      public String getExporter() {
        return exporter;
      }

      public void setExporter(String exporter) {
        this.exporter = exporter;
      }

      public String getEndpoint() {
        return endpoint;
      }

      public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
      }
    }
  }
}
