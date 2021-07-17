package io.javaoperatorsdk.operator.processing;

import static io.javaoperatorsdk.operator.processing.KubernetesResourceUtils.getUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.javaoperatorsdk.operator.TestUtils;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.config.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.config.RetryConfiguration;
import io.javaoperatorsdk.operator.processing.event.EventSource;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ControllerHandlerTest {

  public static final String CUSTOM_EVENT_SOURCE_NAME = "CustomEventSource";
  private final ResourceController controller = mock(ResourceController.class);
  private ControllerHandler handler;

  @BeforeEach
  void setup() {
    ControllerConfiguration config = mock(ControllerConfiguration.class);
    MixedOperation client =
        mock(MixedOperation.class, withSettings().defaultAnswer(RETURNS_DEEP_STUBS));

    when(config.getCustomResourceClass()).thenReturn(CustomResource.class);
    when(config.getRetryConfiguration())
        .thenReturn(mock(RetryConfiguration.class, withSettings().defaultAnswer(RETURNS_DEFAULTS)));
    when(config.getEffectiveNamespaces()).thenReturn(Collections.emptySet());

    handler = new ControllerHandler(controller, config, client);
  }

  @Test
  void creatingAControllerHandlerRegistersItWithTheController() {
    verify(controller, times(1)).init(handler);
  }

  @Test
  public void registersEventSource() {
    EventSource eventSource = mock(EventSource.class);

    handler.registerEventSource(CUSTOM_EVENT_SOURCE_NAME, eventSource);

    Map<String, EventSource> registeredSources = handler.getRegisteredEventSources();
    assertThat(registeredSources.get(CUSTOM_EVENT_SOURCE_NAME)).isEqualTo(eventSource);
    verify(eventSource, times(1)).setEventHandler(eq(handler));
  }

  @Test
  void startShouldCascadeToEventSource() {
    EventSource eventSource = mock(EventSource.class);

    // register event source
    handler.registerEventSource(CUSTOM_EVENT_SOURCE_NAME, eventSource);

    // when handler start method is called, it should start all registered event sources
    handler.start();

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
