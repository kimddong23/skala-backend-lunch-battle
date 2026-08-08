package com.skala.lunch.analysis.dto;

import lombok.*;

/** 개인 편식 지수 — 한 분류에 표를 얼마나 몰아주는가. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PickyIndexDto {
    private Long memberId;
    private String memberName;
    private String department;
    private Long totalVotes;
    private String topCategory;
    private Long topCategoryVotes;
    private Double pickyPercent;  // 최다 분류 비중 (%)
    private String verdict;       // 판정 한마디
}
