package com.leikooo.yupicturebackend.aop.transactionalaop;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Configuration;

@Aspect
@Slf4j
@Configuration
@AllArgsConstructor
public class PostCommitAnnotationAspect {

    private final PostCommitAdapter postCommitAdapter;

    @Around("@annotation(com.leikooo.yupicturebackend.aop.transactionalaop.PostCommit)")
    public Object aroundAdvice(ProceedingJoinPoint pjp) throws Throwable {
        postCommitAdapter.execute(new PjpAfterCommitRunnable(pjp));
        return null;
    }

    private static final class PjpAfterCommitRunnable implements Runnable {
        private final ProceedingJoinPoint pjp;

        public PjpAfterCommitRunnable(ProceedingJoinPoint pjp) {
            this.pjp = pjp;
        }

        @Override
        public void run() {
            try {
                pjp.proceed();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }
}