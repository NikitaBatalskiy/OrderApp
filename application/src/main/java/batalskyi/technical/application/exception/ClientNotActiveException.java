package batalskyi.technical.application.exception;

public class ClientNotActiveException extends RuntimeException {

  public ClientNotActiveException(String message) {
    super(message);
  }
}
