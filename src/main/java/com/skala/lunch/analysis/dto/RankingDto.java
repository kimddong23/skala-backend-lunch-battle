package com.skala.lunch.analysis.dto;

import lombok.*;

/** 식당 종합 랭킹. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RankingDto {
    private Integer rank;
    private Long restaurantId;
    private String restaurantName;
    private String category;
    private Long winCount;        // 우승 횟수
    private Long totalVotes;      // 누적 득표
    private Long candidateCount;  // 후보로 오른 횟수
    private Double avgScore;      // 평균 평점
    private Double winRate;       // 후보로 올랐을 때 우승한 비율 (%)
}
