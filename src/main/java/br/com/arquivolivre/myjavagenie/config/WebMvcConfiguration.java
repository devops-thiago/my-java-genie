package br.com.arquivolivre.myjavagenie.config;

import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Web MVC configuration for serving the React Chat UI. Configures static resource handling and SPA
 * routing.
 */
@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

  /**
   * Configure resource handlers to serve static files from the React build. Static files are served
   * at root (/), while API endpoints are at /api/*.
   */
  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry
        .addResourceHandler("/**")
        .addResourceLocations("classpath:/static/")
        .resourceChain(true)
        .addResolver(
            new PathResourceResolver() {
              @Override
              protected Resource getResource(
                  @NonNull String resourcePath, @NonNull Resource location) throws IOException {
                // Skip resource resolution for API, WebSocket, and Actuator paths
                // These should be handled by their respective handlers/controllers
                if (resourcePath.startsWith("api/")
                    || resourcePath.startsWith("ws/")
                    || resourcePath.startsWith("actuator/")) {
                  return null;
                }

                Resource requestedResource = location.createRelative(resourcePath);

                // If the resource exists, return it
                if (requestedResource.exists() && requestedResource.isReadable()) {
                  return requestedResource;
                }

                // For SPA routing: return index.html for all other non-existent paths
                // This allows React Router to handle client-side routing
                return new ClassPathResource("/static/index.html");
              }
            });
  }

  /** Configure view controllers for root path. */
  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    registry.addViewController("/").setViewName("forward:/index.html");
  }
}
