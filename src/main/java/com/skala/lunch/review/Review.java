package com.skala.lunch.review;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import com.skala.lunch.member.Member;
import com.skala.lunch.restaurant.Restaurant;

/** 식당 평점. 사원 1명이 식당 1곳에 하나만 남긴다 (수정은 가능). */
@Entity
@Table(name = "reviews",
       uniqueConstraints = @UniqueConstraint(
               name = "uk_review_member_restaurant",
               columnNames = {"member_id", "restaurant_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Review {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    /** 1~5점. */
    @Column(nullable = false)
    private Integer score;

    @Column(length = 200)
    private String comment;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
