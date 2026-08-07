package com.skala.lunch.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 하루치 점심 배틀. 날짜당 하나만 존재한다. */
@Entity
@Table(name = "battles",
       uniqueConstraints = @UniqueConstraint(name = "uk_battle_date", columnNames = "battle_date"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Battle {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "battle_date", nullable = false)
    private LocalDate battleDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Status status;

    /** 투표 마감 시각. 지나면 더 이상 투표할 수 없다. */
    @Column(name = "closes_at", nullable = false)
    private LocalDateTime closesAt;

    /** 마감 후 확정된 우승 식당. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_restaurant_id")
    private Restaurant winner;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public enum Status {
        /** 후보 등록·투표 가능. */
        OPEN,
        /** 마감. 우승 확정. */
        CLOSED
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = Status.OPEN;
        }
    }

    public boolean isVotable() {
        return status == Status.OPEN && LocalDateTime.now().isBefore(closesAt);
    }
}
