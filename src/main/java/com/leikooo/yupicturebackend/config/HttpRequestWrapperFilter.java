package com.leikooo.yupicturebackend.config;

import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import com.leikooo.yupicturebackend.manager.auth.SaTokenContextHolder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 请求包装过滤器
 *
 * @author pine
 */
@Order(-1)
@Component
public class HttpRequestWrapperFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest servletRequest = (HttpServletRequest) request;
            String contentType = servletRequest.getHeader(Header.CONTENT_TYPE.getValue());
            if (ContentType.JSON.getValue().equals(contentType) && servletRequest.getServletPath().contains("api")) {
                // 可以再细粒度一些，只有需要进行空间权限校验的接口才需要包一层
                try {
                    chain.doFilter(new RequestWrapper(servletRequest), response);
                } finally {
                    SaTokenContextHolder.clear();
                }
            } else {
                chain.doFilter(request, response);
            }
        }
    }
}
