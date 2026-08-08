package com.skala.lunch.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 경주에 출전한 햄스터 한 마리.
 *
 * 스탯은 경기마다 새로 뽑는다. 같은 식당이라도 어제 길을 잘 찾던 햄스터가
 * 오늘은 막다른 길만 골라 들어갈 수 있다.
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

    /** 판단력. 갈림길에서 최단 경로 쪽을 고를 확률. */
    @Column(nullable = false)
    private Double sense;

    /** 발놀림. 한 걸음을 실제로 내디딜 확률. */
    @Column(nullable = false)
    private Double pace;

    /** 응원(득표)으로 받은 판단력 가산. */
    @Column(name = "cheer_bonus", nullable = false)
    private Double cheerBonus;

    /** 최근 우승으로 깎인 발놀림. 배가 부르면 굼뜨다. */
    @Column(nullable = false)
    private Double handicap;

    /** 실제로 내디딘 걸음 수. 최단 경로와 견주면 얼마나 헤맸는지 나온다. */
    @Column(nullable = false)
    private Integer steps;

    /** 결승 통과 시점. 완주 못 했으면 null. */
    @Column(name = "finish_tick")
    private Integer finishTick;

    @Column(nullable = false)
    private Integer rank;
}
