package com.ajayaventure.exception;

import java.util.ArrayList;
import java.util.List;

/**
 * Field-level validation error container returned to the UI.
 */
public final class FieldError {

    private final String field;
    private final String message;

    public FieldError(String field, String message) {
        this.field = field;
        this.message = message;
    }

    public String getField() {
        return field;
    }

    public String getMessage() {
        return message;
    }

    public static final class Holder {
        private final List<FieldError> errors = new ArrayList<>();

        public Holder add(String field, String message) {
            errors.add(new FieldError(field, message));
            return this;
        }

        public boolean isEmpty() {
            return errors.isEmpty();
        }

        public List<FieldError> getAll() {
            return errors;
        }

        public void throwIfNotEmpty() {
            if (!errors.isEmpty()) {
                throw new ValidationException(errors);
            }
        }
    }

    /** Thrown when one or more field validations fail. */
    public static final class ValidationException extends RuntimeException {
        private final List<FieldError> errors;

        public ValidationException(List<FieldError> errors) {
            super("validation failed");
            this.errors = errors;
        }

        public List<FieldError> getErrors() {
            return errors;
        }
    }
}