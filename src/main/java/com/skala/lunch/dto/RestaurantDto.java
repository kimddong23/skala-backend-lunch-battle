package com.skala.lunch.dto;

import com.skala.lunch.entity.Restaurant;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RestaurantDto {

    private Long id;

    @NotBlank(message = "식당명은 필수입니다")
    @Size(max = 60, message = "식당명은 60자 이하여야 합니다")
    private String name;

    @NotNull(message = "분류는 필수입니다")
    private Restaurant.Category category;

    @NotNull(message = "도보 시간은 필수입니다")
    @Positive(message = "도보 시간은 1분 이상이어야 합니다")
    @Max(value = 120, message = "도보 120분을 넘으면 점심시간에 다녀올 수 없습니다")
    private Integer walkMinutes;

    @NotNull(message = "가격은 필수입니다")
    @Positive(message = "가격은 1원 이상이어야 합니다")
    @Max(value = Restaurant.MAX_PRICE, message = "가격은 100만원 이하여야 합니다")
    private Long price;

    private Boolean active;

    /** 조회 시에만 채워지는 값. */
    private Double avgScore;
    private Long reviewCount;
    private Long winCount;
}
