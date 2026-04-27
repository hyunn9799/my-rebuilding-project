package com.aicc.silverlink.global.config.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload.local-path:./uploads}")
    private String localUploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 로컬 파일 시스템의 uploads 폴더를 /uploads/** URL로 매핑
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + localUploadPath + "/");
    }
}
