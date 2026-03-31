package com.liriosbeauty.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:8080,http://localhost:5500,http://127.0.0.1:5500,https://liriosbeauty.com,https://static-psi-teal.vercel.app}")
    private String[] configuredOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        Set<String> originPatterns = new LinkedHashSet<>();
        originPatterns.addAll(Arrays.asList(configuredOrigins));

        // Keep development and Vercel origins available even if environment variable overrides defaults.
        originPatterns.add("http://localhost:3000");
        originPatterns.add("http://localhost:8080");
        originPatterns.add("http://localhost:5500");
        originPatterns.add("http://127.0.0.1:5500");
        originPatterns.add("https://static-psi-teal.vercel.app");
        originPatterns.add("https://*.vercel.app");

        registry.addMapping("/api/**")
                .allowedOriginPatterns(originPatterns.stream().filter(s -> s != null && !s.isBlank()).toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
