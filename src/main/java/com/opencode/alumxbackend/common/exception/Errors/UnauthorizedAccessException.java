package com.opencode.alumxbackend.common.exception.Errors;


/**
 * class to handle Unauthorized Access Exception
 */
public class UnauthorizedAccessException  extends RuntimeException {
    public UnauthorizedAccessException(String message) {
        super(message);
    }
}