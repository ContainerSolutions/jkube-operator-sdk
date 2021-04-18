package io.javaoperatorsdk.operator.processing;

import static io.javaoperatorsdk.operator.TestUtils.testCustomResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fabric8.kubernetes.client.Watcher;
import io.javaoperatorsdk.operator.processing.event.DefaultEventSourceManager;
import io.javaoperatorsdk.operator.processing.event.Event;
import io.javaoperatorsdk.operator.processing.event.internal.CustomResourceEvent;
import io.javaoperatorsdk.operator.processing.event.internal.TimerEvent;
import io.javaoperatorsdk.operator.processing.event.internal.TimerEventSource;
import io.javaoperatorsdk.operator.processing.retry.GenericRetry;
import io.javaoperatorsdk.operator.sample.simple.TestCustomResource;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DefaultEventHandlerTest {

  private static final Logger log = LoggerFactory.getLogger(DefaultEventHandlerTest.class);

  public static final int FAKE_CONTROLLER_EXECUTION_DURATION = 250;
  public static final int SEPARATE_EXECUTION_TIMEOUT = 450;
  private EventDispatcher eventDispatcherMock = mock(EventDispatcher.class);
  private CustomResourceCache customResourceCache = new CustomResourceCache();
  private DefaultEventSourceManager defaultEventSourceManagerMock =
      mock(DefaultEventSourceManager.class);
  private TimerEventSource retryTimerEventSourceMock = mock(TimerEventSource.class);

  private DefaultEventHandler defaultEventHandler =
      new DefaultEventHandler(customResourceCache, eventDispatcherMock, "Test", null);

  private DefaultEventHandler defaultEventHandlerWithRetry =
      new DefaultEventHandler(
          customResourceCache,
          eventDispatcherMock,
          "Test",
          GenericRetry.defaultLimitedExponentialRetry());

  @BeforeEach
  public void setup() {
    when(defaultEventSourceManagerMock.getRetryTimerEventSource())
        .thenReturn(retryTimerEventSourceMock);
    defaultEventHandler.setEventSourceManager(defaultEventSourceManagerMock);
    defaultEventHandlerWithRetry.setEventSourceManager(defaultEventSourceManagerMock);
  }

  @Test
  public void dispatchesEventsIfNoExecutionInProgress() {
    defaultEventHandler.handleEvent(prepareCREvent());

    verify(eventDispatcherMock, timeout(50).times(1)).handleExecution(any());
  }

  @Test
  public void skipProcessingIfLatestCustomResourceNotInCache() {
    Event event = prepareCREvent();
    customResourceCache.cleanup(event.getRelatedCustomResourceID());

    defaultEventHandler.handleEvent(event);

    verify(eventDispatcherMock, timeout(50).times(0)).handleExecution(any());
  }

  @Test
  public void ifExecutionInProgressWaitsUntilItsFinished() throws InterruptedException {
    String resourceUid = eventAlreadyUnderProcessing();

    defaultEventHandler.handleEvent(nonCREvent(resourceUid));

    verify(eventDispatcherMock, timeout(SEPARATE_EXECUTION_TIMEOUT).times(1))
        .handleExecution(any());
  }

  @Test
  public void buffersAllIncomingEventsWhileControllerInExecution() {
    String resourceUid = eventAlreadyUnderProcessing();

    defaultEventHandler.handleEvent(nonCREvent(resourceUid));
    defaultEventHandler.handleEvent(prepareCREvent(resourceUid));

    ArgumentCaptor<ExecutionScope> captor = ArgumentCaptor.forClass(ExecutionScope.class);
    verify(eventDispatcherMock, timeout(SEPARATE_EXECUTION_TIMEOUT).times(2))
        .handleExecution(captor.capture());
    List<Event> events = captor.getAllValues().get(1).getEvents();
    assertThat(events).hasSize(2);
    assertThat(events.get(0)).isInstanceOf(TimerEvent.class);
    assertThat(events.get(1)).isInstanceOf(CustomResourceEvent.class);
  }

  @Test
  public void cleanUpAfterDeleteEvent() {
    TestCustomResource customResource = testCustomResource();
    customResourceCache.cacheResource(customResource);
    CustomResourceEvent event =
        new CustomResourceEvent(Watcher.Action.DELETED, customResource, null);
    String uid = customResource.getMetadata().getUid();

    defaultEventHandler.handleEvent(event);

    waitMinimalTime();

    verify(defaultEventSourceManagerMock, times(1)).cleanup(uid);
    assertThat(customResourceCache.getLatestResource(uid)).isNotPresent();
  }

  @Test
  public void schedulesAnEventRetryOnException() {
    Event event = prepareCREvent();
    TestCustomResource customResource = testCustomResource();

    ExecutionScope executionScope = new ExecutionScope(Arrays.asList(event), customResource, null);
    PostExecutionControl postExecutionControl =
        PostExecutionControl.exceptionDuringExecution(new RuntimeException("test"));

    defaultEventHandlerWithRetry.eventProcessingFinished(executionScope, postExecutionControl);

    verify(retryTimerEventSourceMock, times(1))
        .scheduleOnce(eq(customResource), eq(GenericRetry.DEFAULT_INITIAL_INTERVAL));
  }

  @Test
  public void executesTheControllerInstantlyAfterErrorIfEventsBuffered() {
    Event event = prepareCREvent();
    TestCustomResource customResource = testCustomResource();
    customResource.getMetadata().setUid(event.getRelatedCustomResourceID());
    ExecutionScope executionScope = new ExecutionScope(Arrays.asList(event), customResource, null);
    PostExecutionControl postExecutionControl =
        PostExecutionControl.exceptionDuringExecution(new RuntimeException("test"));

    // start processing an event
    defaultEventHandlerWithRetry.handleEvent(event);
    // buffer an another event
    defaultEventHandlerWithRetry.handleEvent(event);
    verify(eventDispatcherMock, timeout(SEPARATE_EXECUTION_TIMEOUT).times(1))
        .handleExecution(any());

    defaultEventHandlerWithRetry.eventProcessingFinished(executionScope, postExecutionControl);

    ArgumentCaptor<ExecutionScope> executionScopeArgumentCaptor =
        ArgumentCaptor.forClass(ExecutionScope.class);
    verify(eventDispatcherMock, timeout(SEPARATE_EXECUTION_TIMEOUT).times(2))
        .handleExecution(executionScopeArgumentCaptor.capture());
    List<ExecutionScope> allValues = executionScopeArgumentCaptor.getAllValues();
    assertThat(allValues).hasSize(2);
    assertThat(allValues.get(1).getEvents()).hasSize(2);
    verify(retryTimerEventSourceMock, never())
        .scheduleOnce(eq(customResource), eq(GenericRetry.DEFAULT_INITIAL_INTERVAL));
  }

  @Test
  public void successfulExecutionResetsTheRetry() {
    log.info("Starting successfulExecutionResetsTheRetry");

    Event event = prepareCREvent();
    TestCustomResource customResource = testCustomResource();
    customResource.getMetadata().setUid(event.getRelatedCustomResourceID());
    ExecutionScope executionScope = new ExecutionScope(Arrays.asList(event), customResource, null);
    PostExecutionControl postExecutionControlWithException =
        PostExecutionControl.exceptionDuringExecution(new RuntimeException("test"));
    PostExecutionControl defaultDispatchControl = PostExecutionControl.defaultDispatch();

    when(eventDispatcherMock.handleExecution(any()))
        .thenReturn(postExecutionControlWithException)
        .thenReturn(defaultDispatchControl);

    ArgumentCaptor<ExecutionScope> executionScopeArgumentCaptor =
        ArgumentCaptor.forClass(ExecutionScope.class);

    defaultEventHandlerWithRetry.handleEvent(event);

    verify(eventDispatcherMock, timeout(SEPARATE_EXECUTION_TIMEOUT).times(1))
        .handleExecution(any());
    defaultEventHandlerWithRetry.handleEvent(event);

    verify(eventDispatcherMock, timeout(SEPARATE_EXECUTION_TIMEOUT).times(2))
        .handleExecution(any());
    defaultEventHandlerWithRetry.handleEvent(event);

    verify(eventDispatcherMock, timeout(SEPARATE_EXECUTION_TIMEOUT).times(3))
        .handleExecution(executionScopeArgumentCaptor.capture());
    log.info("Finished successfulExecutionResetsTheRetry");

    List<ExecutionScope> executionScopes = executionScopeArgumentCaptor.getAllValues();

    assertThat(executionScopes).hasSize(3);
    assertThat(executionScopes.get(0).getRetryInfo()).isNull();
    assertThat(executionScopes.get(2).getRetryInfo()).isNull();
    assertThat(executionScopes.get(1).getRetryInfo().getAttemptCount()).isEqualTo(1);
    assertThat(executionScopes.get(1).getRetryInfo().isLastAttempt()).isEqualTo(false);
  }

  private void waitMinimalTime() {
    try {
      Thread.sleep(50);
    } catch (InterruptedException e) {
      throw new IllegalStateException(e);
    }
  }

  private String eventAlreadyUnderProcessing() {
    when(eventDispatcherMock.handleExecution(any()))
        .then(
            (Answer<PostExecutionControl>)
                invocationOnMock -> {
                  Thread.sleep(FAKE_CONTROLLER_EXECUTION_DURATION);
                  return PostExecutionControl.defaultDispatch();
                });
    Event event = prepareCREvent();
    defaultEventHandler.handleEvent(event);
    return event.getRelatedCustomResourceID();
  }

  private CustomResourceEvent prepareCREvent() {
    return prepareCREvent(UUID.randomUUID().toString());
  }

  private CustomResourceEvent prepareCREvent(String uid) {
    TestCustomResource customResource = testCustomResource(uid);
    customResourceCache.cacheResource(customResource);
    return new CustomResourceEvent(Watcher.Action.MODIFIED, customResource, null);
  }

  private Event nonCREvent(String relatedCustomResourceUid) {
    TimerEvent timerEvent = new TimerEvent(relatedCustomResourceUid, null);
    return timerEvent;
  }
}
