package io.javaoperatorsdk.operator.processing.event.internal;

import io.javaoperatorsdk.operator.processing.cache.CustomResourceID;
import io.javaoperatorsdk.operator.processing.event.AbstractEvent;

public class TimerEvent extends AbstractEvent {

  public TimerEvent(CustomResourceID customResourceID, TimerEventSource eventSource) {
    super(customResourceID, eventSource);
  }
}
