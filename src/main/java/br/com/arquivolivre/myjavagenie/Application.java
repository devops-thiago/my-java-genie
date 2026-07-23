package br.com.arquivolivre.myjavagenie;

import br.com.arquivolivre.myjavagenie.config.IngestionConfig;
import br.com.arquivolivre.myjavagenie.config.LlmConfig;
import br.com.arquivolivre.myjavagenie.config.VectorDbConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({LlmConfig.class, IngestionConfig.class, VectorDbConfig.class})
public class Application {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
