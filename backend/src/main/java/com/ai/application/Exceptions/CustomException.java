package com.ai.application.Exceptions;

public class CustomException extends RuntimeException {
	private static final long serialVersionUID = 1L;
    private final String status;

    public CustomException(String message, String status) {
        super(message);
        this.status = status;
    }

    public CustomException(String message) {
        super(message);
        this.status = "error";
    }

    public String getStatus() {
        return status;
    }
}