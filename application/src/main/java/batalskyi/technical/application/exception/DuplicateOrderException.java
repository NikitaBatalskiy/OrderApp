package batalskyi.technical.application.exception;

public class DuplicateOrderException extends RuntimeException {

  public DuplicateOrderException(String message) {
    super(message);
  }
}
