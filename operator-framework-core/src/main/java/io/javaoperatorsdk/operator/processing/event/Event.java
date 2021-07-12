package io.javaoperatorsdk.operator.processing.event;

/**
 * The interface all user-defined events must implement. Also implemented by {@link AbstractEvent}.
 */
public interface Event {

  /**
   * Gets the UID of the custom resource the event describes.
   *
   * @return the UID of the resource
   */
  String getRelatedCustomResourceUid();

  /**
   * Gets the {@link EventSource} that the event originated from
   *
   * @return the originating {@link EventSource}
   */
  EventSource getEventSource();
}
