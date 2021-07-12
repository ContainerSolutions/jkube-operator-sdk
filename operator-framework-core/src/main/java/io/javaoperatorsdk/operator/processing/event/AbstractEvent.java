package io.javaoperatorsdk.operator.processing.event;

/**
 * Abstract class to have user-defined event classes inherit from.
 */
public abstract class AbstractEvent implements Event {

  /**
   * The UID of the custom resource the event describes.
   */
  private final String relatedCustomResourceUid;

  /**
   * The {@link EventSource} the event originates from.
   */
  private final EventSource eventSource;

  /**
   * Instantiates an event object provided the resource the event describes and the event's source.
   *
   * @param relatedCustomResourceUid the UID of the resource
   * @param eventSource the {@link EventSource} that the event originates from
   */
  public AbstractEvent(String relatedCustomResourceUid, EventSource eventSource) {
    this.relatedCustomResourceUid = relatedCustomResourceUid;
    this.eventSource = eventSource;
  }

  /**
   * Gets the UID of the custom resource the event describes from the stored property of the object.
   *
   * @return the UID of the resource as a string
   */
  @Override
  public String getRelatedCustomResourceUid() {
    return relatedCustomResourceUid;
  }

  /**
   * Gets the {@link EventSource} the event originated from from the stored property of the object.
   *
   * @return the originating {@link EventSource}
   */
  @Override
  public EventSource getEventSource() {
    return eventSource;
  }

  /**
   * Provides a text representation of the object that displays the inheriting class name and the
   * values of the object properties.
   *
   * @return the stringified representation of the object
   */
  @Override
  public String toString() {
    return "{ class="
        + this.getClass().getName()
        + ", relatedCustomResourceUid="
        + relatedCustomResourceUid
        + ", eventSource="
        + eventSource
        + " }";
  }
}
