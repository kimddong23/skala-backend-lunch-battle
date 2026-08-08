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

    /**
     * 지금 득표대로 경주에 나갈 때 받게 될 판단력 가산.
     *
     * 표가 결과에 어떻게 작용하는지 화면에서 바로 보이게 하려고 미리 계산해 둔다.
     * 실제 값은 경주 시작 시점의 득표로 다시 계산된다.
     */
    private Double cheerBonus;

}
