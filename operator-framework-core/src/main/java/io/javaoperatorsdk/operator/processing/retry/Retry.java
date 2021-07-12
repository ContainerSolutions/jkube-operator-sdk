package io.javaoperatorsdk.operator.processing.retry;

import io.javaoperatorsdk.operator.api.config.RetryConfiguration;

/**
 * Interface to be implemented by user-defined classes processing retries while executing logic in
 * {@link io.javaoperatorsdk.operator.processing.event.EventHandler}s. Extends
 * {@link RetryConfiguration} that configures how to process retries.
 */
public interface Retry extends RetryConfiguration {

  /**
   * Starts executing retries.
   *
   * @return the {@link RetryExecution} object detailing executed retries
   */
  RetryExecution initExecution();
}
