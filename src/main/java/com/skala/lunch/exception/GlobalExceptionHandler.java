package com.skala.lunch.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 컨트롤러에서 발생한 예외를 한곳에서 받아 상태 코드와 응답 형식을 정한다.
 * 이것이 없으면 조회 실패도, 검증 실패도 전부 500 으로 나간다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** @Valid 검증 실패 — 어느 필드가 왜 틀렸는지 함께 돌려준다. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException e, HttpServletRequest request) {

        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return build(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다", request, errors);
    }

    /**
     * 본문 자체를 읽지 못하는 경우 — JSON 문법 오류, 없는 enum 값, 필드 타입 불일치.
     * 요청이 잘못된 것이므로 400 이다. 이것이 없으면 전부 500 으로 나간다.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(
            HttpMessageNotReadableException e, HttpServletRequest request) {

        log.warn("요청 본문 해석 실패: {}", e.getMessage());
        return build(HttpStatus.BAD_REQUEST, "요청 본문을 해석할 수 없습니다", request, null);
    }

    /** 숫자 자리에 문자를 넣는 등 타입이 맞지 않는 경우. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(
            MethodArgumentTypeMismatchException e, HttpServletRequest request) {

        String message = "'" + e.getName() + "' 값이 올바르지 않습니다: " + e.getValue();
        return build(HttpStatus.BAD_REQUEST, message, request, null);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            NotFoundException e, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, e.getMessage(), request, null);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(
            ConflictException e, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, e.getMessage(), request, null);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> handleBadRequest(
            BadRequestException e, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, e.getMessage(), request, null);
    }

    /**
     * 금액 계산이 long 범위를 넘은 경우.
     * 요청이 잘못된 것이 아니라 보관 중인 값이 계산 가능한 범위를 넘어선 것이므로 500 이다.
     * 다만 "서버 내부 오류" 로 뭉개지 않고 원인을 밝힌다.
     */
    @ExceptionHandler(ArithmeticException.class)
    public ResponseEntity<ApiError> handleArithmetic(
            ArithmeticException e, HttpServletRequest request) {

        log.error("금액 계산 범위 초과", e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "계산 결과가 처리할 수 있는 범위를 벗어났습니다", request, null);
    }

    /** 코드에서 놓친 제약 위반이 DB 까지 내려간 경우의 안전망. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(
            DataIntegrityViolationException e, HttpServletRequest request) {

        // 집계 SQL 이 자리를 넘긴 경우도 여기로 온다. 제약 위반과 원인이 다르므로 구분해서 알린다.
        // (자바 쪽 같은 상황은 ArithmeticException 으로 오며, 두 경로가 같은 메시지를 내도록 맞춤)
        String message = String.valueOf(e.getMessage());
        if (message.contains("out of range") || message.contains("Numeric value")) {
            log.error("집계 결과 범위 초과: {}", message);
            return build(HttpStatus.INTERNAL_SERVER_ERROR,
                    "계산 결과가 처리할 수 있는 범위를 벗어났습니다", request, null);
        }

        log.warn("데이터 제약 위반: {}", message);
        return build(HttpStatus.CONFLICT, "데이터 제약 조건에 걸렸습니다", request, null);
    }

    /**
     * 마지막 안전망.
     * 다만 없는 주소·허용되지 않은 메서드처럼 스프링이 이미 상태 코드를 정해 둔 예외는
     * 그 코드를 그대로 살린다. 전부 500 으로 뭉개면 클라이언트가 원인을 알 수 없다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(
            Exception e, HttpServletRequest request) {

        if (e instanceof org.springframework.web.ErrorResponse springError) {
            HttpStatus status = HttpStatus.valueOf(springError.getStatusCode().value());
            return build(status, e.getMessage(), request, null);
        }

        log.error("처리하지 못한 예외", e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다", request, null);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message,
                                                HttpServletRequest request,
                                                Map<String, String> errors) {
        ApiError body = ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .errors(errors)
                .build();

        return ResponseEntity.status(status).body(body);
    }
}
