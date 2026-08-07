package com.skala.lunch.dto;

import lombok.*;

import java.time.LocalDate;

/** 배틀별 투표 참여율. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ParticipationDto {
    private Long battleId;
    private LocalDate battleDate;
    private String winnerName;
    private Long candidateCount;
    private Long voterCount;
    private Long memberCount;
    private Double participationPercent;
}
