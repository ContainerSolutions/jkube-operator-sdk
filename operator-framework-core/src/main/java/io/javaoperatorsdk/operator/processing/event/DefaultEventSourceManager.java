package io.javaoperatorsdk.operator.processing.event;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.config.ControllerConfiguration;
import io.javaoperatorsdk.operator.processing.CustomResourceCache;
import io.javaoperatorsdk.operator.processing.DefaultEventHandler;
import io.javaoperatorsdk.operator.processing.event.internal.CustomResourceEventSource;
import io.javaoperatorsdk.operator.processing.event.internal.TimerEventSource;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultEventSourceManager implements EventSourceManager {

  public static final String RETRY_TIMER_EVENT_SOURCE_NAME = "retry-timer-event-source";
  private static final Logger log = LoggerFactory.getLogger(DefaultEventSourceManager.class);

  private final ReentrantLock lock = new ReentrantLock();
  private Map<String, EventSource> eventSources = new ConcurrentHashMap<>();
  private CustomResourceEventSource customResourceEventSource;
  private DefaultEventHandler defaultEventHandler;
  private TimerEventSource retryTimerEventSource;

  DefaultEventSourceManager(DefaultEventHandler defaultEventHandler, boolean supportRetry) {
    this.defaultEventHandler = defaultEventHandler;
    defaultEventHandler.setEventSourceManager(this);
    if (supportRetry) {
      this.retryTimerEventSource = new TimerEventSource();
      registerEventSource(RETRY_TIMER_EVENT_SOURCE_NAME, retryTimerEventSource);
    }
  }

  public String[] getTargetNamespaces() {
    return customResourceEventSource.getTargetNamespaces();
  }

  public <R extends CustomResource> DefaultEventSourceManager(
      ResourceController<R> controller,
      ControllerConfiguration<R> configuration,
      CustomResourceCache customResourceCache,
      MixedOperation<R, KubernetesResourceList<R>, Resource<R>> client) {
    this(new DefaultEventHandler(controller, configuration, customResourceCache, client), true);
    // check if we only want to watch the current namespace
    var targetNamespaces = configuration.getNamespaces().toArray(new String[] {});
    if (configuration.watchCurrentNamespace()) {
      targetNamespaces =
          new String[] {
            configuration.getConfigurationService().getClientConfiguration().getNamespace()
          };
    }
    registerCustomResourceEventSource(
        new CustomResourceEventSource(
            customResourceCache,
            client,
            targetNamespaces,
            configuration.isGenerationAware(),
            configuration.getFinalizer()));
  }

  public void registerCustomResourceEventSource(
      CustomResourceEventSource customResourceEventSource) {
    this.customResourceEventSource = customResourceEventSource;
    customResourceEventSource.setEventHandler(defaultEventHandler);
    this.customResourceEventSource.addedToEventManager();
  }

  @Override
  public <T extends EventSource> void registerEventSource(String name, T eventSource) {
    try {
      lock.lock();
      EventSource currentEventSource = eventSources.get(name);
      if (currentEventSource != null) {
        throw new IllegalStateException(
            "Event source with name already registered. Event source name: " + name);
      }
      eventSources.put(name, eventSource);
      eventSource.setEventHandler(defaultEventHandler);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public Optional<EventSource> deRegisterCustomResourceFromEventSource(
      String eventSourceName, String customResourceUid) {
    try {
      lock.lock();
      EventSource eventSource = this.eventSources.get(eventSourceName);
      if (eventSource == null) {
        log.warn(
            "Event producer: {} not found for custom resource: {}",
            eventSourceName,
            customResourceUid);
        return Optional.empty();
      } else {
        eventSource.eventSourceDeRegisteredForResource(customResourceUid);
        return Optional.of(eventSource);
      }
    } finally {
      lock.unlock();
    }
  }

  public TimerEventSource getRetryTimerEventSource() {
    return retryTimerEventSource;
  }

  @Override
  public Map<String, EventSource> getRegisteredEventSources() {
    return Collections.unmodifiableMap(eventSources);
  }

  public void cleanup(String customResourceUid) {
    getRegisteredEventSources()
        .keySet()
        .forEach(k -> deRegisterCustomResourceFromEventSource(k, customResourceUid));
    eventSources.remove(customResourceUid);
  }
}
