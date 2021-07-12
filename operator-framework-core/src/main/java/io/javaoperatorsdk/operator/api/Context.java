package io.javaoperatorsdk.operator.api;

import io.fabric8.kubernetes.client.CustomResource;
import io.javaoperatorsdk.operator.processing.event.EventList;
import java.util.Optional;

/**
 * Encapsulates information about the state of the cluster with respect to the provided custom
 * resource class.
 *
 * @param <T> the {@link CustomResource} for which the context contains information
 */
public interface Context<T extends CustomResource> {

  /**
   * Gets the events related to the relevant custom resource
   *
   * @return the list of relevant {@link io.javaoperatorsdk.operator.processing.event.Event}s
   */
  EventList getEvents();

  /**
   * Retrieves an object describing how to process retries.
   *
   * @return a {@link RetryInfo} object or null
   */
  Optional<RetryInfo> getRetryInfo();
}
