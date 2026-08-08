package com.skala.lunch.battle;

import com.skala.lunch.battle.Battle;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BattleDto {

    private Long id;

    @NotNull(message = "배틀 날짜는 필수입니다")
    private LocalDate battleDate;

    private Battle.Status status;

    /** 미지정 시 설정값(기본 11:30)으로 채워진다. */
    private LocalDateTime closesAt;

    private String winnerName;
    private LocalDateTime closedAt;

    private Long totalVotes;
    private List<CandidateDto> candidates;

    /** 상황에 맞는 한마디. */
    private String comment;

    /** 응원의 효능에 대한 안내 (없음). */
    private String cheerNotice;
}
