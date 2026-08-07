package com.skala.lunch.aop;

import com.skala.lunch.dto.BattleDto;
import com.skala.lunch.dto.VoteRequestDto;
import com.skala.lunch.repository.MemberRepository;
import com.skala.lunch.service.VoteAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 투표가 성공하면 감사 로그를 남긴다.
 *
 * 기록 코드를 vote() 안에 넣지 않는 이유는 투표 로직과 기록 정책의 수명이 다르기 때문이다.
 * 기록 형식이 바뀌어도 투표 코드는 건드리지 않는다.
 *
 * 순서를 트랜잭션 어드바이저보다 앞에 둬서 이 어드바이스가 트랜잭션 프록시 바깥에 놓이게 한다.
 * 그래야 투표가 커밋된 뒤 실행된다.
 * HIGHEST_PRECEDENCE 가 아니라 +1 인 이유는, 최우선 순위를 주면 조인포인트 정보를 넣어주는
 * ExposeInvocationInterceptor 보다 먼저 실행되어 IllegalStateException 이 나기 때문이다.
 */
@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class VoteAuditAspect {

    private final VoteAuditService voteAuditService;
    private final MemberRepository memberRepository;

    @AfterReturning(
            pointcut = "execution(* com.skala.lunch.service.BattleService.vote(..))",
            returning = "result")
    public void writeAuditLog(JoinPoint joinPoint, Object result) {
        if (!(result instanceof BattleDto battle)) {
            return;
        }

        try {
            String voterName = "알 수 없음";
            for (Object arg : joinPoint.getArgs()) {
                if (arg instanceof VoteRequestDto req) {
                    voterName = memberRepository.findById(req.getMemberId())
                            .map(m -> m.getName())
                            .orElse("알 수 없음");
                }
            }

            voteAuditService.record(battle, voterName);
            log.info("[투표감사] {} · {}표 · 1위 {}",
                    voterName, battle.getTotalVotes(),
                    battle.getCandidates() == null || battle.getCandidates().isEmpty()
                            ? "-" : battle.getCandidates().get(0).getRestaurantName());

        } catch (Exception e) {
            // 투표는 이미 커밋되었다. 기록 실패로 응답까지 실패시키지 않는다
            log.error("[투표감사] 기록 실패", e);
        }
    }
}
