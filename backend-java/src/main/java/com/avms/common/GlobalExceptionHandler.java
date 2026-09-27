package com.avms.common;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Failure envelope {success:false,message,errors}, mirroring core/responses.failure. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  record Failure(boolean success, String message, Object errors) {}

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<Failure> notFound(ResourceNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Failure(false, ex.getMessage(), Map.of()));
  }

  @ExceptionHandler(BusinessRuleException.class)
  public ResponseEntity<Failure> business(BusinessRuleException ex) {
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new Failure(false, ex.getMessage(), Map.of()));
  }

  @ExceptionHandler({AccessDeniedException.class, ForbiddenException.class})
  public ResponseEntity<Failure> forbidden(RuntimeException ex) {
    String message = ex.getMessage() != null ? ex.getMessage() : "You do not have permission to perform this action.";
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new Failure(false, message, Map.of()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Failure> validation(MethodArgumentNotValidException ex) {
    Map<String, String> errors = new java.util.HashMap<>();
    ex.getBindingResult().getFieldErrors().forEach(f -> errors.put(f.getField(), f.getDefaultMessage()));
    return ResponseEntity.badRequest().body(new Failure(false, "Validation failed.", errors));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Failure> badRequest(IllegalArgumentException ex) {
    return ResponseEntity.badRequest().body(new Failure(false, ex.getMessage(), Map.of()));
  }

  @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
  public ResponseEntity<Failure> noResource(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Failure(false, "Not found.", Map.of()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Failure> unknown(Exception ex) {
    log.error("Unhandled error", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new Failure(false, "Internal server error.", Map.of()));
  }
}
