package com.leikooo.yupicturebackend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.MappedInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
//@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Bean
    public MappedInterceptor someMethodName() {
        return new MappedInterceptor(
                // => maps to any repository
                new String[]{"/api/**"},
                new JwtInterceptor()
        );
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取 Authorization 头部
        String token = request.getHeader("Authorization");
        // 继续执行请求
        return true;
    }
}
