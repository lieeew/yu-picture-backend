package com.leikooo.yupicturebackend.aop.transactionalaop;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class PostCommitAdapter implements TransactionSynchronization {
    private static final ThreadLocal<List<Runnable>> RUNNABLE = new ThreadLocal<>();

    // register a new runnable for post commit execution
    public void execute(Runnable runnable) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            List<Runnable> runnables = RUNNABLE.get();
            if (runnables == null) {
                runnables = new ArrayList<>();
                runnables.add(runnable);
                RUNNABLE.set(runnables);
                TransactionSynchronizationManager.registerSynchronization(this);
            }
            return;
        }
        // if transaction synchronisation is not active
        runnable.run();
    }

    @Override
    public void afterCommit() {
        List<Runnable> runnables = RUNNABLE.get();
        runnables.forEach(Runnable::run);
    }

    @Override
    public void afterCompletion(int status) {
        RUNNABLE.remove();
    }
}