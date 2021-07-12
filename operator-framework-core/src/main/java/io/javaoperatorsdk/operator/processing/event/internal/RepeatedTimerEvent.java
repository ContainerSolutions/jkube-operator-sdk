package io.javaoperatorsdk.operator.processing.event.internal;

import io.javaoperatorsdk.operator.processing.event.AbstractEvent;

public class RepeatedTimerEvent extends AbstractEvent {

  public RepeatedTimerEvent(String relatedCustomResourceUid, TimerEventSource eventSource) {
    super(relatedCustomResourceUid, eventSource);
  }
}
