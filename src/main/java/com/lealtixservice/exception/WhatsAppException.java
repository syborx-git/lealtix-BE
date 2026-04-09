package com.lealtixservice.exception;

public class WhatsAppException extends RuntimeException {

    private String errorCode;
    private Integer httpStatusCode;

    public WhatsAppException(String message) {
        super(message);
    }

    public WhatsAppException(String message, Throwable cause) {
        super(message, cause);
    }

    public WhatsAppException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public WhatsAppException(String message, String errorCode, Integer httpStatusCode) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatusCode = httpStatusCode;
    }

    public WhatsAppException(String message, String errorCode, Integer httpStatusCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatusCode = httpStatusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }
}
