package com.skala.lunch.global.error;

/**
 * 요청 자체가 성립하지 않을 때. 400 으로 응답한다.
 * 잔액 부족, 보유 수량 부족 등 업무 규칙 위반.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
