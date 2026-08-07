package com.skala.lunch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MemberDto {

    private Long id;

    @NotBlank(message = "로그인 ID는 필수입니다")
    @Size(min = 2, max = 50, message = "로그인 ID는 2자 이상 50자 이하여야 합니다")
    private String loginId;

    @NotBlank(message = "이름은 필수입니다")
    @Size(max = 50, message = "이름은 50자 이하여야 합니다")
    private String name;

    @NotBlank(message = "부서는 필수입니다")
    @Size(max = 50, message = "부서는 50자 이하여야 합니다")
    private String department;
}
