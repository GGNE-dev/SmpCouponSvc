package org.ggne.test.common.aop;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.ggne.test.common.exception.BusinessException;
import org.ggne.test.common.exception.ErrorCode;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@Order(1) // Transactional(기본값 Ordered.LOWEST_PRECEDENCE)보다 먼저 실행되도록 설정
@RequiredArgsConstructor
@Slf4j
public class DistributedLockAspect {

    private final RedissonClient redissonClient;
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(org.ggne.test.common.aop.DistributedLock)")
    public Object lock(final ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        DistributedLock distributedLock = method.getAnnotation(DistributedLock.class);

        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
        String key = getDynamicValue(parameterNames, joinPoint.getArgs(), distributedLock.key());
        
        RLock rLock = redissonClient.getLock(key);

        try {
            log.info("Try Lock for key: {}", key);
            boolean available = rLock.tryLock(distributedLock.waitTime(), distributedLock.leaseTime(), distributedLock.timeUnit());
            if (!available) {
                log.warn("Lock acquisition failed for key: {}", key);
                throw new BusinessException(ErrorCode.COUPON_OUT_OF_STOCK);
            }

            log.info("Lock acquired for key: {}", key);
            return joinPoint.proceed(); // Transactional보다 먼저 실행되었으므로 여기서 Transactional 프록시를 탑니다.
        } finally {
            try {
                rLock.unlock();
                log.info("Lock released for key: {}", key);
            } catch (IllegalMonitorStateException e) {
                log.info("Redisson Lock Already Unlocked");
            }
        }
    }

    private String getDynamicValue(String[] parameterNames, Object[] args, String key) {
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext();

        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }

        return parser.parseExpression(key).getValue(context, String.class);
    }
}
