package com.skala.lunch.repository;

import com.skala.lunch.entity.VoteAuditLog;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoteAuditLogRepository extends JpaRepository<VoteAuditLog, Long> {
    List<VoteAuditLog> findByBattleId(Long battleId, Sort sort);
}
