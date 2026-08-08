package com.skala.lunch.restaurant;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** 후보로 올릴 수 있는 식당. */
@Entity
@Table(name = "restaurants")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Restaurant {

    /** 1인 가격 상한. 금액 합계가 자리를 넘지 않도록 하는 안전장치. */
    public static final long MAX_PRICE = 1_000_000L;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Category category;

    /** 회사에서 걸어서 몇 분. 멀면 비 오는 날 표를 못 받는다. */
    @Column(name = "walk_minutes", nullable = false)
    private Integer walkMinutes;

    /** 1인 평균 가격 (원). */
    @Column(name = "price", nullable = false)
    private Long price;

    /** 폐업하거나 질려서 후보에서 빼고 싶을 때 false. */
    @Column(nullable = false)
    private Boolean active;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public enum Category {
        한식, 중식, 일식, 양식, 분식, 아시안, 샐러드, 패스트푸드
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (active == null) {
            active = true;
        }
    }
}
