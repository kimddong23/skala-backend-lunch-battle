package com.skala.lunch.battle;

import com.skala.lunch.battle.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {
    Optional<Vote> findByBattleIdAndMemberId(Long battleId, Long memberId);
    boolean existsByBattleIdAndMemberId(Long battleId, Long memberId);
    long countByBattleId(Long battleId);
    long countByCandidateId(Long candidateId);
    long countByMemberId(Long memberId);
    List<Vote> findByBattleId(Long battleId);
}
