package io.javaoperatorsdk.operator.api;

import io.fabric8.kubernetes.client.CustomResource;
import io.javaoperatorsdk.operator.processing.event.EventList;
import java.util.Optional;

/**
 * Default implementation of the {@link Context} interface to be used by users when no custom
 * implementation of that interface is needed.
 *
 * @param <T> the {@link CustomResource} the context is built around
 */
public class DefaultContext<T extends CustomResource> implements Context<T> {

  /**
   * Object containing information on how retries are processed.
   */
  private final RetryInfo retryInfo;

  /**
   * List of events relating to the custom resource in context.
   */
  private final EventList events;

  /**
   * Provides an instance given a list of events and the object encapsulating information about
   * retries being currently processed, both relating to the given custom resource.
   *
   * @param events the {@link EventList} containing the relevant events to the resource
   * @param retryInfo the {@link RetryInfo} object encapsulating info about retries
   */
  public DefaultContext(EventList events, RetryInfo retryInfo) {
    this.retryInfo = retryInfo;
    this.events = events;
  }

  /**
   * Gets the events associated with this context instance.
   *
   * @return the {@link EventList} associated with this instance
   */
  @Override
  public EventList getEvents() {
    return events;
  }

  /**
   * Gets the retry information associated with this context instance.
   *
   * @return the {@link RetryInfo} object associated with this instance
   */
  @Override
  public Optional<RetryInfo> getRetryInfo() {
    return Optional.ofNullable(retryInfo);
  }
}
