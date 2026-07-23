package br.com.arquivolivre.myjavagenie.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "opentelemetry")
public class OpenTelemetryProperties {

  private boolean enabled = true;
  private String serviceName = "my-java-genie";

  /** logging (console) or otlp */
  private String exporter = "logging";

  private String otlpEndpoint = "http://localhost:4317";
  private long metricsExportIntervalMillis = 10_000L;

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

  public String getExporter() {
    return exporter;
  }

  public void setExporter(String exporter) {
    this.exporter = exporter;
  }

  public String getOtlpEndpoint() {
    return otlpEndpoint;
  }

  public void setOtlpEndpoint(String otlpEndpoint) {
    this.otlpEndpoint = otlpEndpoint;
  }

  public long getMetricsExportIntervalMillis() {
    return metricsExportIntervalMillis;
  }

  public void setMetricsExportIntervalMillis(long metricsExportIntervalMillis) {
    this.metricsExportIntervalMillis = metricsExportIntervalMillis;
  }
}
