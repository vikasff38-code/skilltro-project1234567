package org.skilltro;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class WebConfig implements WebMvcConfigurer {
  private final List<String> allowedOrigins;
  WebConfig(@Value("${skilltro.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173,http://localhost:4173}") String origins){
    this.allowedOrigins=Arrays.stream(origins.split(",")).map(String::trim).filter(s->!s.isEmpty()).toList();
  }
  @Override public void addCorsMappings(CorsRegistry registry){
    registry.addMapping("/api/**").allowedOrigins(allowedOrigins.toArray(String[]::new))
        .allowedMethods("GET","POST","PUT","DELETE","OPTIONS")
        .allowedHeaders("Content-Type","Accept")
        .allowCredentials(true).maxAge(3600);
  }
}
