package com.skala.lunch.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 햄스터 레이스. 배틀 하나당 한 번 달린다.
 *
 * 스탯은 출발 직전에 난수로 정해진다. 만든 사람도 결과를 미리 알 수 없다.
 * 대신 사용한 시드를 남겨 두어 같은 레이스를 언제든 다시 돌려볼 수 있게 한다 —
 * "짜고 친 것 아니냐" 는 의심에 답할 수 있어야 한다.
 */
@Entity
@Table(name = "races",
       uniqueConstraints = @UniqueConstraint(name = "uk_race_battle", columnNames = "battle_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Race {

    /** 결승선까지의 거리. */
    public static final int TRACK_LENGTH = 1000;

    /** 한 경기의 최대 진행 수. 이 안에 못 들어오면 강제 종료한다. */
    public static final int MAX_TICKS = 400;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "battle_id", nullable = false)
    private Battle battle;

    /** 난수 시드. 이 값이 있으면 같은 경기를 그대로 재현할 수 있다. */
    @Column(nullable = false)
    private Long seed;

    /** 결승선을 통과하기까지 걸린 진행 수. */
    @Column(name = "total_ticks", nullable = false)
    private Integer totalTicks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_restaurant_id")
    private Restaurant winner;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "race", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RaceLane> lanes = new ArrayList<>();

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
