package me.fengorz.kiwi.ai.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents the result of a validation operation
 * 
 * @Author Kason Zhan
 * @Date 06/10/2025
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ValidationResult {
    
    private final boolean valid;
    private final String errorMessage;
    private final String errorCode;

    /**
     * Creates a successful validation result
     * @return ValidationResult indicating success
     */
    public static ValidationResult valid() {
        return new ValidationResult(true, null, null);
    }

    /**
     * Creates a failed validation result with error details
     * @param errorMessage the error message
     * @param errorCode the error code
     * @return ValidationResult indicating failure
     */
    public static ValidationResult invalid(String errorMessage, String errorCode) {
        return new ValidationResult(false, errorMessage, errorCode);
    }

    /**
     * Checks if the validation was successful
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Checks if the validation failed
     * @return true if invalid, false otherwise
     */
    public boolean isInvalid() {
        return !valid;
    }
}