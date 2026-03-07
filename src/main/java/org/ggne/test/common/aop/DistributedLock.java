package org.ggne.test.common.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {
    /**
     * 락의 이름 (예: "coupon:1")
     */
    String key();

    /**
     * 락 획득을 위해 기다리는 시간 (기본 5초)
     */
    long waitTime() default 5L;

    /**
     * 락을 획득한 후 점유하는 시간 (기본 3초)
     */
    long leaseTime() default 3L;

    /**
     * 시간 단위 (기본 초 단위)
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
