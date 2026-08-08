package com.skala.lunch.analysis.dto;

import lombok.*;

/**
 * 식당 목록용 집계.
 *
 * 평점·리뷰 수·우승 횟수를 식당마다 따로 조회하면 식당 수만큼 질의가 늘어난다(N+1).
 * SQL 한 문장으로 한 번에 가져온다.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RestaurantSummaryDto {
    private Long id;
    private String name;
    private String category;
    private Integer walkMinutes;
    private Long price;
    private Boolean active;
    private Double avgScore;
    private Long reviewCount;
    private Long winCount;
}
