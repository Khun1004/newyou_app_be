package com.newyou.sms;

/** 문자 발송 실패 시 발생하는 예외 */
public class SmsSendException extends RuntimeException {
    public SmsSendException(String message) {
        super(message);
    }

    public SmsSendException(String message, Throwable cause) {
        super(message, cause);
    }
}