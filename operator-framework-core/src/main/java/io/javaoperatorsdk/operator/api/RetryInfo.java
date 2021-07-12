package io.javaoperatorsdk.operator.api;

/**
 * Interface to be implemented by user-defined classes encapsulating information about how many
 * retries have been processed.
 */
public interface RetryInfo {

  /**
   * Gets how many attempts have been made so far.
   *
   * @return the number of attempts
   */
  int getAttemptCount();

  /**
   * Gets whether the current attempt is the last attempt to be made.
   *
   * @return whether the current one is the last attempt
   */
  boolean isLastAttempt();
}
