package com.leikooo.yupicturebackend.controller;

import com.leikooo.yupicturebackend.commen.BaseResponse;
import com.leikooo.yupicturebackend.commen.ResultUtils;
import com.leikooo.yupicturebackend.async.AsyncHelper;
import com.leikooo.yupicturebackend.service.SpaceService;
import org.springframework.aop.framework.AopContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.concurrent.CompletableFuture;

/**
 * @author leikooo
 */
@RestController
@RequestMapping("/")
public class MainController {
    @Resource
    private SpaceService spaceService;

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public BaseResponse<String> health() {
        return ResultUtils.success("ok");
    }

//    @GetMapping("/test")
//    public Object asyncCall() throws InterruptedException {
//        System.out.println("before....");
//        ((MainController)(AopContext.currentProxy())).testAsyncTask();
//        System.out.println("after....");
//        return "OK";
//    }
//
//    @Async
//    public void testAsyncTask() throws InterruptedException {
//        Thread.sleep(10000);
//        System.out.println("异步任务执行完成！");
//    }
//
//    protected CompletableFuture<String> doSupplyAsyncUsingHelper() {
//        return asyncHelper.supplyAsync(() -> {
//            try {
//                testAsyncTask();
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//            return "Something";
//        });
//    }

//    protected void doRunAsyncUsingHelper() {
//        asyncHelper.runAsync(() -> {
//            doSomething();
//            doSomethingElse();
//        });
//    }

    @GetMapping("/test2")
    public Object test() throws InterruptedException {
        spaceService.validSpace(null, true);
        return "OK";
    }

}
