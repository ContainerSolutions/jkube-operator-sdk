package io.javaoperatorsdk.operator.processing.event;

import static io.javaoperatorsdk.operator.processing.KubernetesResourceUtils.getUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.fabric8.kubernetes.client.CustomResource;
import io.javaoperatorsdk.operator.TestUtils;
import io.javaoperatorsdk.operator.processing.DefaultEventHandler;
import io.javaoperatorsdk.operator.processing.KubernetesResourceUtils;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ControllerHandlerTest {

  public static final String CUSTOM_EVENT_SOURCE_NAME = "CustomEventSource";

  private DefaultEventHandler mockEventHandler = mock(DefaultEventHandler.class);
  private ControllerHandler handler = new ControllerHandler(mockEventHandler, false);

  @Test
  public void registersEventSource() {
    EventSource eventSource = mock(EventSource.class);

    handler.registerEventSource(CUSTOM_EVENT_SOURCE_NAME, eventSource);

    Map<String, EventSource> registeredSources = handler.getRegisteredEventSources();
    assertThat(registeredSources.entrySet()).hasSize(1);
    assertThat(registeredSources.get(CUSTOM_EVENT_SOURCE_NAME)).isEqualTo(eventSource);
    verify(eventSource, times(1)).setEventHandler(eq(mockEventHandler));
    verify(eventSource, times(1)).start();
  }

  @Test
  public void closeShouldCascadeToEventSources() {
    EventSource eventSource = mock(EventSource.class);
    EventSource eventSource2 = mock(EventSource.class);
    handler.registerEventSource(CUSTOM_EVENT_SOURCE_NAME, eventSource);
    handler.registerEventSource(CUSTOM_EVENT_SOURCE_NAME + "2", eventSource2);

    handler.close();

    verify(eventSource, times(1)).close();
    verify(eventSource2, times(1)).close();
  }

  @Test
  public void throwExceptionIfRegisteringEventSourceWithSameName() {
    EventSource eventSource = mock(EventSource.class);
    EventSource eventSource2 = mock(EventSource.class);

    handler.registerEventSource(CUSTOM_EVENT_SOURCE_NAME, eventSource);
    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(
            () -> {
              handler.registerEventSource(CUSTOM_EVENT_SOURCE_NAME, eventSource2);
            });
  }

  @Test
  public void deRegistersEventSources() {
    CustomResource customResource = TestUtils.testCustomResource();
    EventSource eventSource = mock(EventSource.class);
    handler.registerEventSource(CUSTOM_EVENT_SOURCE_NAME, eventSource);

    handler.deRegisterCustomResourceFromEventSource(
        CUSTOM_EVENT_SOURCE_NAME, getUID(customResource));

    verify(eventSource, times(1))
        .eventSourceDeRegisteredForResource(eq(KubernetesResourceUtils.getUID(customResource)));
  }
}
