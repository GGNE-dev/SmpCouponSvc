package org.ggne.test.common.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * AOP에서 트랜잭션 분리를 위한 별도 클래스
 */
@Component
public class CallByDistributedLockTransaction {

    /**
     * REQUIRES_NEW를 사용하여 락 획득 후 새로운 트랜잭션을 시작합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Object proceed(final ProceedingJoinPoint joinPoint) throws Throwable {
        return joinPoint.proceed();
    }
}
