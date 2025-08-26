package batalskyi.technical.application.exception;

import batalskyi.technical.application.dto.ApiError;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Log4j2
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ClientNotFoundException.class)
  public ResponseEntity<ApiError> handleClientNotFound(ClientNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ApiError(HttpStatus.NOT_FOUND.value(), e.getMessage()));
  }

  @ExceptionHandler(ClientNotActiveException.class)
  public ResponseEntity<ApiError> handleClientNotActive(ClientNotActiveException e) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(new ApiError(HttpStatus.FORBIDDEN.value(), e.getMessage()));
  }

  @ExceptionHandler(DuplicateEmailException.class)
  public ResponseEntity<ApiError> handleDuplicateEmail(DuplicateEmailException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ApiError(HttpStatus.CONFLICT.value(), ex.getMessage()));
  }

  @ExceptionHandler(InvalidPriceException.class)
  public ResponseEntity<ApiError> handleInvalidPrice(InvalidPriceException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ApiError(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
  }

  @ExceptionHandler(ClientProfitLimitExceededException.class)
  public ResponseEntity<ApiError> handleProfitLimit(ClientProfitLimitExceededException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ApiError(HttpStatus.CONFLICT.value(), ex.getMessage()));
  }

  @ExceptionHandler(AttributeMismatchException.class)
  public ResponseEntity<ApiError> handleInvalidAttributesPassed(AttributeMismatchException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ApiError(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleInvalidEmailPassed() {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ApiError(HttpStatus.BAD_REQUEST.value(), "Invalid email address."));
  }

  @ExceptionHandler(DuplicateOrderException.class)
  public ResponseEntity<ApiError> handleDuplicateOrder(DuplicateOrderException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ApiError(HttpStatus.CONFLICT.value(), ex.getMessage()));
  }
}
