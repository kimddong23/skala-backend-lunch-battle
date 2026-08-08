package com.skala.lunch.analysis.dto;

import lombok.*;

/** 부서별 취향 — 어느 부서가 어떤 분류에 표를 몰아주는가. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DepartmentTasteDto {
    private String department;
    private String category;
    private Long voteCount;
    private Double sharePercent;  // 그 부서 전체 표 중 비중
}
