package com.leikooo.yupicturebackend.aop.transactionalaop;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 * @date 2024/12/30
 * @description
 */
@Component
@Slf4j
public class TestAfterCommitterOperation {

    @Transactional
    public void transactionalMethod(Object object) {
        // someOperation(entity)
        log.info("inside transaction");
        TransactionSynchronizationManager.isSynchronizationActive();
        // 获取当前代理对象
        ((TestAfterCommitterOperation) AopContext.currentProxy()).log();
        // save(entity);
        log.info("end of method");
    }


    public void nonTransactionalMethod(Object object) {
        // someOperation(entity)
        log.info("inside transaction");
        log();
        // save(entity);
        log.info("end of method");
    }

    @PostCommit
    public void log() {
        log.info("log from class TestAfterCommitterOperation");
    }
}
