package com.testing.automation.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Map /videos/** to the local videos/ directory
        String videoPath = Paths.get("videos").toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/videos/**")
                .addResourceLocations(videoPath);

        // Map /screenshots/** to the local screenshots/ directory
        String screenshotPath = Paths.get("screenshots").toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/screenshots/**")
                .addResourceLocations(screenshotPath);
    }
}
