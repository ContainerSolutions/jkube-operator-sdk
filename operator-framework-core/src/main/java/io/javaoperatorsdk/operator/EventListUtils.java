package io.javaoperatorsdk.operator;

import io.fabric8.kubernetes.client.Watcher;
import io.javaoperatorsdk.operator.processing.event.Event;
import io.javaoperatorsdk.operator.processing.event.internal.CustomResourceEvent;
import java.util.List;

/**
 * Static class to group together utility methods relating to lists of events.
 */
public class EventListUtils {

  /**
   * Returns whether the provided list of events contains any "Resource Deleted" events
   * @param events the event list to be checked
   * @return the boolean answer
   */
  public static boolean containsCustomResourceDeletedEvent(List<Event> events) {
    return events.stream()
        .anyMatch(
            e -> {
              if (e instanceof CustomResourceEvent) {
                return ((CustomResourceEvent) e).getAction() == Watcher.Action.DELETED;
              } else {
                return false;
              }
            });
  }
}
