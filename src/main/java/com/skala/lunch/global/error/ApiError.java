package com.skala.lunch.global.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 오류 응답의 공통 형식.
 * 값이 없는 항목은 응답에서 제외해 상황별로 필요한 것만 담는다.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    private final LocalDateTime timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final String path;

    /** 입력값 검증 실패일 때만 채워지는 필드별 오류 메시지. */
    private final Map<String, String> errors;
}
