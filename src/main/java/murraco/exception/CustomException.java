package murraco.exception;

public class CustomException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final String message;
  private final int status;

  public CustomException(String message, int status) {
    super(message);
    this.message = message;
    this.status = status;
  }

  @Override
  public String getMessage() {
    return message;
  }

  public int getStatus() {
    return status;
  }
}
