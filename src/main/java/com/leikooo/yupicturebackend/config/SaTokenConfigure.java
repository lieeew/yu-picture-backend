package com.leikooo.yupicturebackend.config;

import com.leikooo.yupicturebackend.manager.auth.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;


@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {
    @Resource
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {;
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**");
    }
}
