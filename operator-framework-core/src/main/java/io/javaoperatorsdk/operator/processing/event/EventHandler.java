package io.javaoperatorsdk.operator.processing.event;

import java.io.Closeable;

public interface EventHandler extends Closeable {

  void handleEvent(Event event);

  @Override
  default void close() {}

  default void start() {}
}
