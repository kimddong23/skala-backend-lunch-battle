package com.skala.lunch.repository;

import com.skala.lunch.entity.Candidate;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, Long> {

    List<Candidate> findByBattleIdOrderByVoteCountDescIdAsc(Long battleId);
    Optional<Candidate> findByBattleIdAndRestaurantId(Long battleId, Long restaurantId);
    boolean existsByBattleIdAndRestaurantId(Long battleId, Long restaurantId);
    long countByBattleId(Long battleId);
    long countByRestaurantId(Long restaurantId);

    /**
     * 득표 수를 올리기 위한 잠금 조회.
     * 잠그지 않으면 동시 투표가 같은 득표 수를 읽고 각자 +1 해 표가 사라진다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Candidate c where c.id = :id")
    Optional<Candidate> findByIdForUpdate(@Param("id") Long id);
}
