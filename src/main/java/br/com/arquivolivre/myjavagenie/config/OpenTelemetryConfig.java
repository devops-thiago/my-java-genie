package br.com.arquivolivre.myjavagenie.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.logging.LoggingMetricExporter;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.semconv.ResourceAttributes;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenTelemetryConfig {

  private static final Logger logger = LoggerFactory.getLogger(OpenTelemetryConfig.class);

  @Bean(destroyMethod = "close")
  public OpenTelemetrySdk openTelemetrySdk(OpenTelemetryProperties properties) {
    if (!properties.isEnabled()) {
      logger.info("OpenTelemetry disabled");
      return OpenTelemetrySdk.builder().build();
    }

    Resource resource =
        Resource.getDefault()
            .merge(
                Resource.create(
                    Attributes.of(ResourceAttributes.SERVICE_NAME, properties.getServiceName())));

    boolean useOtlp = "otlp".equalsIgnoreCase(properties.getExporter());
    SpanExporter spanExporter;
    MetricExporter metricExporter;
    if (useOtlp) {
      logger.info("OpenTelemetry exporting via OTLP to {}", properties.getOtlpEndpoint());
      spanExporter =
          OtlpGrpcSpanExporter.builder()
              .setEndpoint(properties.getOtlpEndpoint())
              .setTimeout(10, TimeUnit.SECONDS)
              .build();
      metricExporter =
          OtlpGrpcMetricExporter.builder()
              .setEndpoint(properties.getOtlpEndpoint())
              .setTimeout(10, TimeUnit.SECONDS)
              .build();
    } else {
      logger.info("OpenTelemetry exporting spans/metrics to logging (console)");
      spanExporter = LoggingSpanExporter.create();
      metricExporter = LoggingMetricExporter.create();
    }

    SdkTracerProvider tracerProvider =
        SdkTracerProvider.builder()
            .setResource(resource)
            .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
            .build();

    SdkMeterProvider meterProvider =
        SdkMeterProvider.builder()
            .setResource(resource)
            .registerMetricReader(
                PeriodicMetricReader.builder(metricExporter)
                    .setInterval(Duration.ofMillis(properties.getMetricsExportIntervalMillis()))
                    .build())
            .build();

    return OpenTelemetrySdk.builder()
        .setTracerProvider(tracerProvider)
        .setMeterProvider(meterProvider)
        .buildAndRegisterGlobal();
  }

  @Bean
  public OpenTelemetry openTelemetry(OpenTelemetrySdk sdk) {
    return sdk;
  }

  @Bean
  public Tracer tracer(OpenTelemetry openTelemetry) {
    return openTelemetry.getTracer("my-java-genie");
  }

  @Bean
  public Meter meter(OpenTelemetry openTelemetry) {
    return openTelemetry.getMeter("my-java-genie");
  }
}
