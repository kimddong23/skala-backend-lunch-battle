package com.skala.lunch.analysis.dto;

import lombok.*;

/** 요일별 인기 분류. 월요일엔 국물, 금요일엔 고기 같은 경향을 본다. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WeekdayTrendDto {
    private Integer weekday;      // 1=일 … 7=토 (H2 DAY_OF_WEEK)
    private String weekdayName;
    private String category;
    private Long voteCount;
}
