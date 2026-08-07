package com.skala.lunch.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 투표 1건.
 *
 * (배틀, 사원) 유일 제약이 "1인 1표" 를 DB 수준에서 보장한다.
 * 응용 코드의 중복 검사만으로는 동시 요청 두 개가 모두 통과할 수 있다.
 */
@Entity
@Table(name = "votes",
       uniqueConstraints = @UniqueConstraint(
               name = "uk_vote_battle_member",
               columnNames = {"battle_id", "member_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Vote {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "battle_id", nullable = false)
    private Battle battle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @Column(name = "voted_at")
    private LocalDateTime votedAt;

    @PrePersist
    void onCreate() {
        votedAt = LocalDateTime.now();
    }
}
