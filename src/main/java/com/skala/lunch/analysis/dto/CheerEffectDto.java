package com.skala.lunch.analysis.dto;

import lombok.*;

import java.time.LocalDate;

/**
 * 응원 무용지수.
 *
 * 배틀마다 "표를 가장 많이 받은 메뉴"와 "실제로 이긴 메뉴"를 나란히 놓는다.
 * 경주가 득표와 무관하다는 주장을 말로 하지 않고 기록으로 보여 주기 위한 집계다.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CheerEffectDto {
    private LocalDate battleDate;
    private String favoriteName;    // 최다 득표 메뉴
    private Long favoriteVotes;
    private String winnerName;      // 실제 우승 메뉴
    private Boolean matched;        // 둘이 같았나
    private String verdict;
}
