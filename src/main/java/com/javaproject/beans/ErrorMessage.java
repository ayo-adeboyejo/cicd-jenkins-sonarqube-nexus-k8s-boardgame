package com.javaproject.beans;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Standard error response body for REST endpoints.
 */
@Data
@AllArgsConstructor
public class ErrorMessage {
    private final String status = "error";
    private String message;
}
