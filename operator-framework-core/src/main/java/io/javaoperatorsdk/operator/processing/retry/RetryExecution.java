package io.javaoperatorsdk.operator.processing.retry;

import io.javaoperatorsdk.operator.api.RetryInfo;
import java.util.Optional;

/**
 * Interface to be implemented by user-defined objects detailing scheduling a currently retried
 * execution. Extends the {@link RetryInfo} interface, as it details retry attempts made in the
 * past.
 */
public interface RetryExecution extends RetryInfo {

  /**
   * Calculates the delay for the next execution, also called on the first execution.
   *
   * @return the delay before the next execution in seconds
   */
  Optional<Long> nextDelay();
}
