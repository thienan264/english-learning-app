package com.project.englishlearning.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final CourseAccessInterceptor access;
    private final SiteActivityInterceptor activity;
    public WebConfig(CourseAccessInterceptor access, SiteActivityInterceptor activity) { this.access = access; this.activity=activity; }
    @Override
    public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        registry.addInterceptor(activity).addPathPatterns("/**");
        registry.addInterceptor(access).addPathPatterns("/learn/**", "/lessons/**", "/courses/*/vocabulary-quiz/**", "/api/flashcards/*/flip");
    }


    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadPath = System.getProperty("user.dir") + "/uploads/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath);
    }
}
