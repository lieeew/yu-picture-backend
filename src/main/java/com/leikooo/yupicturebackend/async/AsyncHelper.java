package com.leikooo.yupicturebackend.async;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 */
@Slf4j
@Component
public class AsyncHelper {

    @Async
    public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        log.info("supplyAsync invoked");
        return CompletableFuture.supplyAsync(supplier);
    }

    @Async
    public void runAsync(Runnable runnable) {
        log.info("runAsync invoked");
        CompletableFuture.runAsync(runnable);
    }
}