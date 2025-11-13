package br.com.arquivolivre.myjavagenie;

import br.com.arquivolivre.myjavagenie.config.IngestionConfig;
import br.com.arquivolivre.myjavagenie.config.ModelConfig;
import br.com.arquivolivre.myjavagenie.config.QueryConfig;
import br.com.arquivolivre.myjavagenie.config.VectorDbConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the Java RAG System application.
 * This Spring Boot application provides a Retrieval-Augmented Generation system
 * for querying Java 25 documentation using natural language.
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
        ModelConfig.class,
        VectorDbConfig.class,
        IngestionConfig.class,
        QueryConfig.class
})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
