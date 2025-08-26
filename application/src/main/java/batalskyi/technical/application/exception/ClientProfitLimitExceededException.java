package batalskyi.technical.application.exception;

public class ClientProfitLimitExceededException extends RuntimeException {

  public ClientProfitLimitExceededException(String message) {
    super(message);
  }
}
