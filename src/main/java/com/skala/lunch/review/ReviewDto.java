package com.skala.lunch.review;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewDto {

    private Long id;

    @NotNull(message = "작성자 ID는 필수입니다")
    private Long memberId;

    private String memberName;

    @NotNull(message = "식당 ID는 필수입니다")
    private Long restaurantId;

    private String restaurantName;

    @NotNull(message = "점수는 필수입니다")
    @Min(value = 1, message = "점수는 1점 이상이어야 합니다")
    @Max(value = 5, message = "점수는 5점 이하여야 합니다")
    private Integer score;

    @Size(max = 200, message = "한줄평은 200자 이하여야 합니다")
    private String comment;

    private LocalDateTime createdAt;
}
