package io.javaoperatorsdk.operator.processing.event;

import io.javaoperatorsdk.operator.processing.cache.CustomResourceID;

public interface Event {

  CustomResourceID getRelatedCustomResourceID();

  EventSource getEventSource();
}
