package com.skala.lunch.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 투표 감사 로그.
 *
 * 누가 언제 무엇에 투표했는지 남긴다. 투표 서비스에는 기록 코드가 없고,
 * AOP 가 투표 성공을 감지해 대신 남긴다.
 */
@Entity
@Table(name = "vote_audit_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VoteAuditLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "battle_id", nullable = false)
    private Long battleId;

    @Column(nullable = false, length = 200)
    private String message;

    /** 그 시점의 1위 식당. 판이 어떻게 뒤집혔는지 나중에 볼 수 있다. */
    @Column(name = "leading_restaurant", length = 60)
    private String leadingRestaurant;

    @Column(name = "total_votes")
    private Integer totalVotes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
