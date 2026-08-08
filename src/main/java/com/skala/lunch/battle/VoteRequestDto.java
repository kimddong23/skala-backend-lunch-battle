package com.skala.lunch.battle;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VoteRequestDto {

    @NotNull(message = "투표자 ID는 필수입니다")
    private Long memberId;

    @NotNull(message = "후보 ID는 필수입니다")
    private Long candidateId;
}
