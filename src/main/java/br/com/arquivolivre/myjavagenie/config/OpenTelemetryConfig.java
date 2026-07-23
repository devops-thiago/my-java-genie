package br.com.arquivolivre.myjavagenie.config;

import br.com.arquivolivre.myjavagenie.util.LogSanitizer;
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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for OpenTelemetry observability. Sets up traces, metrics, and logs exporters
 * with OTLP protocol.
 */
@Configuration
@ConditionalOnProperty(name = "opentelemetry.enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(OpenTelemetryConfig.OpenTelemetryProperties.class)
public class OpenTelemetryConfig {

  private static final Logger logger = LoggerFactory.getLogger(OpenTelemetryConfig.class);

  private final OpenTelemetryProperties properties;

  public OpenTelemetryConfig(OpenTelemetryProperties properties) {
    this.properties = properties;
    logger.info(
        "Initializing OpenTelemetry with service name: {}",
        LogSanitizer.sanitize(properties.serviceName()));
  }

  /** Creates the OpenTelemetry SDK instance with configured exporters. */
  @Bean
  public OpenTelemetry openTelemetry() {
    Resource resource =
        Resource.getDefault()
            .merge(
                Resource.create(
                    Attributes.builder()
                        .put(ResourceAttributes.SERVICE_NAME, properties.serviceName())
                        .put(ResourceAttributes.SERVICE_VERSION, properties.serviceVersion())
                        .put(ResourceAttributes.DEPLOYMENT_ENVIRONMENT, properties.environment())
                        .build()));

    var sdkBuilder = OpenTelemetrySdk.builder();

    // Configure Tracer Provider
    if (properties.traces().enabled()) {
      SdkTracerProvider tracerProvider = configurTracerProvider(resource);
      sdkBuilder.setTracerProvider(tracerProvider);
      logger.info(
          "OpenTelemetry traces enabled with endpoint: {}",
          LogSanitizer.sanitize(properties.traces().endpoint()));
    }

    // Configure Meter Provider
    if (properties.metrics().enabled()) {
      SdkMeterProvider meterProvider = configureMeterProvider(resource);
      sdkBuilder.setMeterProvider(meterProvider);
      logger.info(
          "OpenTelemetry metrics enabled with endpoint: {}",
          LogSanitizer.sanitize(properties.metrics().endpoint()));
    }

    // Configure Logger Provider
    if (properties.logs().enabled()) {
      SdkLoggerProvider loggerProvider = configureLoggerProvider(resource);
      sdkBuilder.setLoggerProvider(loggerProvider);
      logger.info(
          "OpenTelemetry logs enabled with endpoint: {}",
          LogSanitizer.sanitize(properties.logs().endpoint()));
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
          LogSanitizer.sanitize(alreadyRegistered.getMessage()));
    }

    logger.info("OpenTelemetry SDK initialized successfully");
    return openTelemetry;
  }

  /** Configures the tracer provider with OTLP exporter. */
  private SdkTracerProvider configurTracerProvider(Resource resource) {
    try {
      OtlpGrpcSpanExporter spanExporter =
          OtlpGrpcSpanExporter.builder()
              .setEndpoint(properties.traces().endpoint())
              .setTimeout(10, TimeUnit.SECONDS)
              .build();

      return SdkTracerProvider.builder()
          .setResource(resource)
          .addSpanProcessor(
              BatchSpanProcessor.builder(spanExporter)
                  .setScheduleDelay(Duration.ofSeconds(5))
                  .build())
          .setSampler(Sampler.traceIdRatioBased(properties.traces().samplingRate()))
          .build();
    } catch (Exception e) {
      logger.error(
          "Failed to configure tracer provider, traces will not be exported: {}",
          LogSanitizer.sanitize(e.getMessage()));
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
              .setEndpoint(properties.metrics().endpoint())
              .setTimeout(10, TimeUnit.SECONDS)
              .build();

      return SdkMeterProvider.builder()
          .setResource(resource)
          .registerMetricReader(
              PeriodicMetricReader.builder(metricExporter)
                  .setInterval(Duration.ofMillis(properties.metrics().exportIntervalMillis()))
                  .build())
          .build();
    } catch (Exception e) {
      logger.error(
          "Failed to configure meter provider, metrics will not be exported: {}",
          LogSanitizer.sanitize(e.getMessage()));
      // Return a no-op meter provider to allow application to continue
      return SdkMeterProvider.builder().setResource(resource).build();
    }
  }

  /** Configures the logger provider with OTLP exporter. */
  private SdkLoggerProvider configureLoggerProvider(Resource resource) {
    try {
      OtlpGrpcLogRecordExporter logExporter =
          OtlpGrpcLogRecordExporter.builder()
              .setEndpoint(properties.logs().endpoint())
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
          "Failed to configure logger provider, logs will not be exported: {}",
          LogSanitizer.sanitize(e.getMessage()));
      // Return a no-op logger provider to allow application to continue
      return SdkLoggerProvider.builder().setResource(resource).build();
    }
  }

  /** Creates a Tracer bean for manual instrumentation. */
  @Bean
  public Tracer tracer(OpenTelemetry openTelemetry) {
    return openTelemetry.getTracer(properties.serviceName());
  }

  /** Creates a Meter bean for custom metrics. */
  @Bean
  public Meter meter(OpenTelemetry openTelemetry) {
    return openTelemetry.getMeter(properties.serviceName());
  }

  /**
   * Immutable configuration properties for OpenTelemetry. Populated through Spring Boot constructor
   * binding, falling back to the declared defaults when properties are absent.
   */
  @ConfigurationProperties(prefix = "opentelemetry")
  public record OpenTelemetryProperties(
      @DefaultValue("true") boolean enabled,
      @DefaultValue("java-rag-system") String serviceName,
      @DefaultValue("1.0.0") String serviceVersion,
      @DefaultValue("development") String environment,
      @DefaultValue TracesConfig traces,
      @DefaultValue MetricsConfig metrics,
      @DefaultValue LogsConfig logs) {

    /** Trace exporter configuration. */
    public record TracesConfig(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("otlp") String exporter,
        @DefaultValue("http://localhost:4317") String endpoint,
        @DefaultValue("1.0") double samplingRate) {}

    /** Metric exporter configuration. */
    public record MetricsConfig(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("otlp") String exporter,
        @DefaultValue("http://localhost:4317") String endpoint,
        @DefaultValue("60000") long exportIntervalMillis) {}

    /** Log exporter configuration. */
    public record LogsConfig(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("otlp") String exporter,
        @DefaultValue("http://localhost:4317") String endpoint) {}
  }
}
