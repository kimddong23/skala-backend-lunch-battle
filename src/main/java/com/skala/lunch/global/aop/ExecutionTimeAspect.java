package com.skala.lunch.global.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import com.skala.lunch.battle.BattleRules;

/**
 * 서비스 계층 전체의 실행 시간을 남기는 공통 관심사.
 *
 * 측정 코드를 메서드마다 넣으면 업무 코드에 계측이 섞이고, 메서드가 늘 때마다 빠뜨리게 된다.
 * 어디에 적용할지(포인트컷)와 무엇을 할지(어드바이스)를 분리해 한곳에서 관리한다.
 */
@Slf4j
@Aspect
@Component
public class ExecutionTimeAspect {

    /**
     * 이름이 Service 로 끝나는 클래스의 public 메서드만.
     *
     * 이름이 Service 로 끝나는 클래스만 잡는다. 도메인 패키지 전체를 대상으로 두면
     * 설정값 클래스(BattleRules)의 getter 까지 계측되어 로그의 3분의 2가 설정 조회로 채워진다.
     * 보고 싶은 것은 업무 처리이지 설정 조회가 아니다.
     */
    @Pointcut("execution(public * com.skala.lunch..*Service.*(..))")
    public void serviceLayer() {
    }

    @Around("serviceLayer()")
    public Object measure(ProceedingJoinPoint joinPoint) throws Throwable {
        String target = joinPoint.getSignature().getDeclaringType().getSimpleName()
                + "." + joinPoint.getSignature().getName();
        long startedAt = System.nanoTime();

        try {
            Object result = joinPoint.proceed();
            log.info("[실행시간] {}({}) → {}ms",
                    target, formatArgs(joinPoint.getArgs()), elapsedMillis(startedAt));
            return result;

        } catch (Throwable e) {
            // 예외도 시간과 함께 남긴다. 실패가 느린 것인지 즉시 튕긴 것인지 구분하기 위함
            log.warn("[실행실패] {}({}) → {}ms, {}: {}",
                    target, formatArgs(joinPoint.getArgs()), elapsedMillis(startedAt),
                    e.getClass().getSimpleName(), e.getMessage());
            throw e;
        }
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    /** 인자를 짧게 요약한다. 엔티티가 통째로 찍히면 로그가 읽기 어려워진다. */
    private String formatArgs(Object[] args) {
        return Arrays.stream(args)
                .map(arg -> arg == null ? "null" : summarize(arg))
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    private String summarize(Object arg) {
        if (arg instanceof Number || arg instanceof CharSequence || arg instanceof Enum<?>) {
            return String.valueOf(arg);
        }
        return arg.getClass().getSimpleName();
    }
}
