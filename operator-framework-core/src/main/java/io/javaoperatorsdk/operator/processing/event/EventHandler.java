package io.javaoperatorsdk.operator.processing.event;

import java.io.Closeable;

/**
 * Interface to be implemented by user-defined event handler classes that encapsulate logic to be
 * run as a response to cluster events. Implements Closeable so that a shutting down
 * {@link io.javaoperatorsdk.operator.Operator}/{@link io.javaoperatorsdk.operator.api.Controller}
 * can shut down all dependent event handling.
 */
public interface EventHandler extends Closeable {

  /**
   * Function that gets called as a response to cluster events to execute logic in response to them.
   *
   * @param event the {@link Event} instance detailing the cluster event
   */
  void handleEvent(Event event);

  /**
   * Provides a default empty implementation to satisfy Closeable even if there is nothing to do on
   * shutdown.
   */
  @Override
  default void close() {}
}
