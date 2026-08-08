package com.skala.lunch.global.error;

/**
 * 현재 자원 상태와 요청이 충돌할 때. 409 로 응답한다.
 * 중복 등록, 참조가 남아 있는 대상의 삭제 시도 등.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
