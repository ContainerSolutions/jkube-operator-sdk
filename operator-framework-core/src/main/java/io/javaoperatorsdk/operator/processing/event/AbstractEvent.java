package io.javaoperatorsdk.operator.processing.event;

import io.javaoperatorsdk.operator.processing.cache.CustomResourceID;

public abstract class AbstractEvent implements Event {

  private final CustomResourceID relatedCustomResourceUid;

  private final EventSource eventSource;

  public AbstractEvent(CustomResourceID relatedCustomResourceUid, EventSource eventSource) {
    this.relatedCustomResourceUid = relatedCustomResourceUid;
    this.eventSource = eventSource;
  }

  @Override
  public CustomResourceID getRelatedCustomResourceID() {
    return relatedCustomResourceUid;
  }

  @Override
  public EventSource getEventSource() {
    return eventSource;
  }

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
