package com.skala.lunch.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 레이스에 출전한 햄스터 한 마리.
 *
 * 스탯은 경기마다 새로 뽑는다. 같은 식당이라도 어제 빠르던 햄스터가 오늘 느릴 수 있다.
 */
@Entity
@Table(name = "race_lanes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RaceLane {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "race_id", nullable = false)
    private Race race;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    /** 기본 속도. */
    @Column(nullable = false)
    private Double speed;

    /** 지구력. 낮으면 후반에 처진다. */
    @Column(nullable = false)
    private Double stamina;

    /** 순간 가속이 터질 확률. */
    @Column(nullable = false)
    private Double burst;

    /** 응원(득표)으로 받은 가산. */
    @Column(name = "cheer_bonus", nullable = false)
    private Double cheerBonus;

    /** 최근 우승으로 짊어진 짐. 무거울수록 느리다. */
    @Column(nullable = false)
    private Double handicap;

    /** 결승 통과 시점. 완주 못 했으면 null. */
    @Column(name = "finish_tick")
    private Integer finishTick;

    @Column(nullable = false)
    private Integer rank;
}
