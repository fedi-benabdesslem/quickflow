package com.ai.application.model;

/**
 * Response DTO for generated meeting minutes content.
 */
public class GeneratedMinutes {
    private String content;
    private String status;
    private String message;

    public GeneratedMinutes() {
    }

    public GeneratedMinutes(String content) {
        this.content = content;
        this.status = "success";
    }

    public static GeneratedMinutes success(String content) {
        GeneratedMinutes response = new GeneratedMinutes();
        response.setContent(content);
        response.setStatus("success");
        return response;
    }

    public static GeneratedMinutes error(String message) {
        GeneratedMinutes response = new GeneratedMinutes();
        response.setStatus("error");
        response.setMessage(message);
        return response;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
