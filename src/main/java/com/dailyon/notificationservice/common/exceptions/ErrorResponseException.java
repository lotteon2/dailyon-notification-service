package com.dailyon.notificationservice.common.exceptions;

public class ErrorResponseException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "올바르지 않은 요청입니다.";
    public ErrorResponseException() {
        super(DEFAULT_MESSAGE);
    }

    public ErrorResponseException(String message) {
        super(message);
    }
}