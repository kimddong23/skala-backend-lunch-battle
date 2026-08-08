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


}
