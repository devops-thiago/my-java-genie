package br.com.arquivolivre.myjavagenie;

import br.com.arquivolivre.myjavagenie.config.LlmConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(LlmConfig.class)
public class Application {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
