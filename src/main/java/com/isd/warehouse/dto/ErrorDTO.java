package com.isd.warehouse.dto;

import java.time.LocalDateTime;

public class ErrorDTO {
    private String message;
    private LocalDateTime timestamp;
    // Todo: use lombok
    // Todo: validation errors
    public ErrorDTO(String message, LocalDateTime timestamp) {
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
