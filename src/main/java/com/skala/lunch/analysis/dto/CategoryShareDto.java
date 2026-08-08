package com.skala.lunch.analysis.dto;

import lombok.*;

/** 분류별 득표 점유율 — 회사 전체 입맛 지도. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CategoryShareDto {
    private String category;
    private Long voteCount;
    private Long winCount;
    private Double sharePercent;
}
