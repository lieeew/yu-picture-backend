package com.leikooo.yupicturebackend.controller;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import com.leikooo.yupicturebackend.commen.BaseResponse;
import com.leikooo.yupicturebackend.commen.ResultUtils;
import com.leikooo.yupicturebackend.manager.auth.StpKit;
import com.leikooo.yupicturebackend.service.SpaceService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @author leikooo
 */
@RestController
@RequestMapping("/api/main")
public class MainController {

    // 测试登录，浏览器访问
    @PostMapping("doLogin")
    public String doLogin(String username, String password) {
        // 此处仅作模拟示例，真实项目需要从数据库中查询数据进行比对
        if ("zhang".equals(username) && "123456".equals(password)) {
            StpKit.SPACE.login(1001);
            return "登录成功";
        }
        return "登录失败";
    }

    // 查询登录状态，浏览器访问
    @GetMapping("isLogin")
    public String isLogin() {
        return "当前会话是否登录：" + StpKit.SPACE.isLogin();
    }

    // 查询 Token 信息
    @GetMapping("tokenInfo")
    public SaResult tokenInfo() {
        StpKit.SPACE.checkLogin();
        return SaResult.data(StpKit.SPACE.getTokenInfo());
    }

    // 测试注销
    @GetMapping("logout")
    public SaResult logout() {
        StpUtil.logout();
        return SaResult.ok();
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public BaseResponse<String> health() {
        return ResultUtils.success("ok");
    }

}
