package com.leikooo.yupicturebackend.controller;

import com.leikooo.yupicturebackend.commen.BaseResponse;
import com.leikooo.yupicturebackend.commen.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author leikooo
 */
@RestController
@RequestMapping("/")
public class MainController {

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public BaseResponse<String> health(HttpServletRequest request, HttpServletResponse response) {
        System.out.println("request = " + request);
        System.out.println("response = " + response);
        String header = response.getHeader("X-Custom-Header");
        return ResultUtils.success("ok");
    }
}
