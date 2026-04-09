package com.shoes.ecommerce.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadPath = System.getProperty("user.dir") + "/uploads/";
        // also serve uploads from parent workspace folder in case server working dir differs
        String parentUpload = java.nio.file.Paths.get(System.getProperty("user.dir")).getParent().resolve("uploads").toAbsolutePath().toString() + "/";
        registry.addResourceHandler("/uploads/**").addResourceLocations("file:" + uploadPath, "file:" + parentUpload);
        System.out.println("[WebConfig] Serving uploads from: " + uploadPath + " and " + parentUpload);
    }
}
