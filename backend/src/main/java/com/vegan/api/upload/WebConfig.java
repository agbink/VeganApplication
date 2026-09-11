package com.vegan.api.upload;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * /uploads/** URL로 요청이 오면 서버 로컬의 ./uploads/ 폴더에서 파일을 찾아 반환합니다.
 * 예) GET http://서버주소:8080/uploads/abc123.jpg
 *     → 서버의 ./uploads/abc123.jpg 파일을 응답
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // file: 프로토콜로 절대/상대 경로를 지정 (끝에 / 필수)
        String location = "file:" + uploadDir + "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}
