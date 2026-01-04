package com.geocom.retail.domain.exception;

/**
 * Exception thrown when a date range is invalid.
 */
public class InvalidDateRangeException extends DomainException {

    public InvalidDateRangeException(String message) {
        super(message);
    }
}
