package com.skala.lunch.dto;

import com.skala.lunch.entity.Restaurant;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CandidateDto {

    private Long id;
    private Long restaurantId;
    private String restaurantName;
    private Restaurant.Category category;
    private Integer walkMinutes;
    private Long price;

    private String addedByName;
    private Integer voteCount;

    /** 득표율 (%). */
    private Double sharePercent;

    /** 최근에 우승해서 깎인 점수. 0이면 불이익 없음. */
    private Integer repeatPenalty;

    /** 감점을 반영한 최종 점수 — 순위는 이 값으로 매긴다. */
    private Integer finalScore;

    /** 감점 사유 안내. */
    private String penaltyNote;
}
