package com.skala.lunch.repository;

import com.skala.lunch.entity.Battle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BattleRepository extends JpaRepository<Battle, Long> {
    Optional<Battle> findByBattleDate(LocalDate battleDate);
    boolean existsByBattleDate(LocalDate battleDate);
    List<Battle> findByStatusOrderByBattleDateDesc(Battle.Status status);
    List<Battle> findAllByOrderByBattleDateDesc();

    /** 최근 우승 식당 조회용 — "또 그거야?" 감점 계산에 쓴다. */
    List<Battle> findByStatusAndBattleDateGreaterThanEqualOrderByBattleDateDesc(
            Battle.Status status, LocalDate from);

    long countByWinnerId(Long restaurantId);
}
