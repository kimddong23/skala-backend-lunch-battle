package com.skala.lunch.battle;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import com.skala.lunch.member.Member;
import com.skala.lunch.restaurant.Restaurant;

/**
 * 배틀에 오른 후보 식당.
 *
 * 같은 배틀에 같은 식당이 두 번 오르면 표가 갈리므로 유일 제약으로 막는다.
 * (동시에 두 사람이 같은 식당을 올리는 경우가 실제로 있다)
 */
@Entity
@Table(name = "candidates",
       uniqueConstraints = @UniqueConstraint(
               name = "uk_candidate_battle_restaurant",
               columnNames = {"battle_id", "restaurant_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Candidate {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "battle_id", nullable = false)
    private Battle battle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    /** 이 후보를 올린 사람. 우승하면 공을, 망하면 원망을 받는다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "added_by_member_id", nullable = false)
    private Member addedBy;

    /**
     * 집계용 득표 수.
     *
     * votes 테이블을 세면 되지만, 순위 조회가 잦아 후보 행에 함께 둔다.
     * 이 값을 고치는 것이 곧 투표이므로 동시 투표는 이 행을 잠그고 처리한다.
     */
    @Column(name = "vote_count", nullable = false)
    private Integer voteCount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (voteCount == null) {
            voteCount = 0;
        }
    }
}
