package com.opencode.alumxbackend.common.exception.Errors;

import lombok.NoArgsConstructor;

/**
 * class to handle Bad Request Exception
 */
@NoArgsConstructor
public class BadRequestException  extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}