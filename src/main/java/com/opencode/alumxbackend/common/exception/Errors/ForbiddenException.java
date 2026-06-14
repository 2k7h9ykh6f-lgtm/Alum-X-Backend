package com.opencode.alumxbackend.common.exception.Errors;

import lombok.NoArgsConstructor;

/**
 * Exception thrown when a user is authenticated but lacks permission to access a resource
 */
@NoArgsConstructor
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
