package io.javaoperatorsdk.operator.processing.event.internal;

import static io.javaoperatorsdk.operator.processing.KubernetesResourceUtils.getUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.fabric8.kubernetes.client.CustomResource;
import io.javaoperatorsdk.operator.TestUtils;
import io.javaoperatorsdk.operator.processing.KubernetesResourceUtils;
import io.javaoperatorsdk.operator.processing.event.EventHandler;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@Disabled(
    "Currently very flaky, will fix in https://github.com/java-operator-sdk/java-operator-sdk/issues/293")
class TimerEventSourceTest {

  public static final int INITIAL_DELAY = 50;
  public static final int PERIOD = 50;
  public static final int TESTING_TIME_SLACK = 40;

  private TimerEventSource timerEventSource;
  private EventHandler eventHandlerMock = mock(EventHandler.class);

  @BeforeEach
  public void setup() {
    timerEventSource = new TimerEventSource();
    timerEventSource.setEventHandler(eventHandlerMock);
  }

  @Test
  public void producesEventsPeriodically() {
    CustomResource customResource = TestUtils.testCustomResource();

    timerEventSource.schedule(customResource, INITIAL_DELAY, PERIOD);

    ArgumentCaptor<TimerEvent> argumentCaptor = ArgumentCaptor.forClass(TimerEvent.class);
    verify(eventHandlerMock, timeout(INITIAL_DELAY + PERIOD + TESTING_TIME_SLACK).times(2))
        .handleEvent(argumentCaptor.capture());
    List<TimerEvent> events = argumentCaptor.getAllValues();
    assertThat(events)
        .allMatch(e -> e.getRelatedCustomResourceID().equals(getUID(customResource)));
    assertThat(events).allMatch(e -> e.getEventSource().equals(timerEventSource));
  }

  @Test
  public void deRegistersPeriodicalEventSources() throws InterruptedException {
    CustomResource customResource = TestUtils.testCustomResource();

    timerEventSource.schedule(customResource, INITIAL_DELAY, PERIOD);
    Thread.sleep(INITIAL_DELAY + PERIOD + TESTING_TIME_SLACK);
    timerEventSource.eventSourceDeRegisteredForResource(getUID(customResource));
    Thread.sleep(PERIOD + TESTING_TIME_SLACK);

    verify(eventHandlerMock, times(2)).handleEvent(any());
  }

  @Test
  public void schedulesOnce() throws InterruptedException {
    CustomResource customResource = TestUtils.testCustomResource();

    timerEventSource.scheduleOnce(customResource, PERIOD);

    Thread.sleep(2 * PERIOD + TESTING_TIME_SLACK);
    verify(eventHandlerMock, times(1)).handleEvent(any());
  }

  @Test
  public void canCancelOnce() throws InterruptedException {
    CustomResource customResource = TestUtils.testCustomResource();

    timerEventSource.scheduleOnce(customResource, PERIOD);
    timerEventSource.cancelOnceSchedule(KubernetesResourceUtils.getUID(customResource));

    Thread.sleep(PERIOD + TESTING_TIME_SLACK);
    verify(eventHandlerMock, never()).handleEvent(any());
  }

  @Test
  public void canRescheduleOnceEvent() throws InterruptedException {
    CustomResource customResource = TestUtils.testCustomResource();

    timerEventSource.scheduleOnce(customResource, PERIOD);
    timerEventSource.scheduleOnce(customResource, 2 * PERIOD);

    Thread.sleep(PERIOD + TESTING_TIME_SLACK);
    verify(eventHandlerMock, never()).handleEvent(any());
    Thread.sleep(PERIOD + TESTING_TIME_SLACK);
    verify(eventHandlerMock, times(1)).handleEvent(any());
  }

  @Test
  public void deRegistersOnceEventSources() throws InterruptedException {
    CustomResource customResource = TestUtils.testCustomResource();

    timerEventSource.scheduleOnce(customResource, PERIOD);
    timerEventSource.eventSourceDeRegisteredForResource(getUID(customResource));
    Thread.sleep(PERIOD + TESTING_TIME_SLACK);

    verify(eventHandlerMock, never()).handleEvent(any());
  }
}
