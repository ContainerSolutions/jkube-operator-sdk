package io.javaoperatorsdk.operator.processing.event;

import java.util.List;
import java.util.Optional;

/**
 * Encapsulates a list of {@link Event}s and related functionality.
 */
public class EventList {

  /**
   * The list of {@link Event}s encapsulated.
   */
  private final List<Event> eventList;

  /**
   * Creates an instance out of a provided list of {@link Event}s
   *
   * @param eventList the list of {@link Event}s
   */
  public EventList(List<Event> eventList) {
    this.eventList = eventList;
  }

  /**
   * Gets the encapsulated list of {@link Event}s.
   *
   * @return the list of {@link Event}s
   */
  public List<Event> getList() {
    return eventList;
  }

  /**
   * Gets the last {@link Event} that occurred that matches the provided type.
   *
   * @param eventType the class of the sought {@link Event}
   * @param <T> the class that the sought {@link Event} should be returned as
   * @return the sought {@link Event}
   */
  public <T extends Event> Optional<T> getLatestOfType(Class<T> eventType) {
    for (int i = eventList.size() - 1; i >= 0; i--) {
      Event event = eventList.get(i);
      if (event.getClass().isAssignableFrom(eventType)) {
        return (Optional<T>) Optional.of(event);
      }
    }
    return Optional.empty();
  }
}
