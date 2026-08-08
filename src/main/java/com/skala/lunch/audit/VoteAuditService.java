package com.skala.lunch.audit;

import com.skala.lunch.battle.BattleDto;
import com.skala.lunch.battle.CandidateDto;
import com.skala.lunch.audit.VoteAuditLog;
import com.skala.lunch.audit.VoteAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 투표 감사 로그 기록.
 *
 * REQUIRES_NEW 로 별도 트랜잭션에서 저장한다.
 * 기록은 투표와 독립적으로 남아야 하고, 반대로 기록에 실패했다고
 * 이미 성사된 투표를 되돌려서는 안 되기 때문이다.
 */
@Service
@RequiredArgsConstructor
public class VoteAuditService {

    private final VoteAuditLogRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(BattleDto battle, String voterName) {
        CandidateDto leading = battle.getCandidates() == null || battle.getCandidates().isEmpty()
                ? null : battle.getCandidates().get(0);

        repository.save(VoteAuditLog.builder()
                .battleId(battle.getId())
                .message(voterName + " 님이 투표했습니다")
                .leadingRestaurant(leading == null ? null : leading.getRestaurantName())
                .totalVotes(battle.getTotalVotes() == null ? 0 : battle.getTotalVotes().intValue())
                .build());
    }
}
