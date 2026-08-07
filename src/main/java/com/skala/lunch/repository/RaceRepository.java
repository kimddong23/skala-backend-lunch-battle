package com.skala.lunch.repository;

import com.skala.lunch.entity.Race;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RaceRepository extends JpaRepository<Race, Long> {

    Optional<Race> findByBattleId(Long battleId);
    boolean existsByBattleId(Long battleId);

    /** 레인까지 한 번에 — 레인마다 따로 읽으면 질의가 출전 수만큼 늘어난다. */
    @Query("select distinct r from Race r left join fetch r.lanes where r.battle.id = :battleId")
    Optional<Race> findWithLanesByBattleId(Long battleId);
}
