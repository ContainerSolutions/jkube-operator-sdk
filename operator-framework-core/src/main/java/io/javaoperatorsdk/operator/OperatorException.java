package io.javaoperatorsdk.operator;


/**
 * Exception class to throw for all operator specific errors.
 */
public class OperatorException extends RuntimeException {

  /**
   * Instantiates an OperatorException with empty message and no attached cause.
   */
  public OperatorException() {}

  /**
   * Instantiates an OperatorException with the provided message and no attached cause.
   * @param message message explaining why the exception is to be thrown
   */
  public OperatorException(String message) {
    super(message);
  }

  /**
   * Instantiates an OperatorException with the provided message and cause
   * @param message message explaining why the exception is to be thrown
   * @param cause the original exception that is being handled while this one is thrown
   */
  public OperatorException(String message, Throwable cause) {
    super(message, cause);
  }
}
