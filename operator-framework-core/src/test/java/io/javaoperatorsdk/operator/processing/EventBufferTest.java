package io.javaoperatorsdk.operator.processing;

import static org.assertj.core.api.Assertions.assertThat;

import io.javaoperatorsdk.operator.processing.event.Event;
import io.javaoperatorsdk.operator.processing.event.internal.TimerEvent;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventBufferTest {

  private EventBuffer eventBuffer = new EventBuffer();

  String uid = UUID.randomUUID().toString();
  Event testEvent1 = new TimerEvent(uid, null);
  Event testEvent2 = new TimerEvent(uid, null);

  @Test
  public void storesEvents() {
    eventBuffer.addEvent(testEvent1);
    eventBuffer.addEvent(testEvent2);

    assertThat(eventBuffer.containsEvents(testEvent1.getRelatedCustomResourceID())).isTrue();
    List<Event> events = eventBuffer.getAndRemoveEventsForExecution(uid);
    assertThat(events).hasSize(2);
  }

  @Test
  public void getsAndRemovesEvents() {
    eventBuffer.addEvent(testEvent1);
    eventBuffer.addEvent(testEvent2);

    List<Event> events = eventBuffer.getAndRemoveEventsForExecution(uid);
    assertThat(events).hasSize(2);
    assertThat(events).contains(testEvent1, testEvent2);
  }

  @Test
  public void checksIfThereAreStoredEvents() {
    eventBuffer.addEvent(testEvent1);
    eventBuffer.addEvent(testEvent2);

    assertThat(eventBuffer.containsEvents(testEvent1.getRelatedCustomResourceID())).isTrue();
  }

  @Test
  public void canClearEvents() {
    eventBuffer.addEvent(testEvent1);
    eventBuffer.addEvent(testEvent2);

    eventBuffer.cleanup(uid);

    assertThat(eventBuffer.containsEvents(testEvent1.getRelatedCustomResourceID())).isFalse();
  }
}
