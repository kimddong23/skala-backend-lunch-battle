package com.skala.lunch.exception;

/**
 * 요청한 자원이 없을 때. 404 로 응답한다.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
