package io.javaoperatorsdk.operator.processing.event.internal;

import static io.javaoperatorsdk.operator.processing.KubernetesResourceUtils.getUID;
import static io.javaoperatorsdk.operator.processing.KubernetesResourceUtils.getVersion;

import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.internal.CustomResourceOperationsImpl;
import io.javaoperatorsdk.operator.processing.CustomResourceCache;
import io.javaoperatorsdk.operator.processing.KubernetesResourceUtils;
import io.javaoperatorsdk.operator.processing.cache.PassThroughResourceCache;
import io.javaoperatorsdk.operator.processing.event.AbstractEventSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This is a special case since is not bound to a single custom resource */
public class CustomResourceEventSource extends AbstractEventSource
    implements Watcher<CustomResource> {

  private static final Logger log = LoggerFactory.getLogger(CustomResourceEventSource.class);

  private final PassThroughResourceCache resourceCache;
  private MixedOperation client;
  private final String[] targetNamespaces;
  private final boolean generationAware;
  private final String resourceFinalizer;
  private final Map<String, Long> lastGenerationProcessedSuccessfully = new ConcurrentHashMap<>();

  public static CustomResourceEventSource customResourceEventSourceForAllNamespaces(
          PassThroughResourceCache customResourceCache,
      MixedOperation client,
      boolean generationAware,
      String resourceFinalizer) {
    return new CustomResourceEventSource(
        customResourceCache, client, null, generationAware, resourceFinalizer);
  }

  public static CustomResourceEventSource customResourceEventSourceForTargetNamespaces(
          PassThroughResourceCache customResourceCache,
      MixedOperation client,
      String[] namespaces,
      boolean generationAware,
      String resourceFinalizer) {
    return new CustomResourceEventSource(
        customResourceCache, client, namespaces, generationAware, resourceFinalizer);
  }

  private CustomResourceEventSource(
          PassThroughResourceCache customResourceCache,
      MixedOperation client,
      String[] targetNamespaces,
      boolean generationAware,
      String resourceFinalizer) {
    this.resourceCache = customResourceCache;
    this.client = client;
    this.targetNamespaces = targetNamespaces;
    this.generationAware = generationAware;
    this.resourceFinalizer = resourceFinalizer;
  }

  private boolean isWatchAllNamespaces() {
    return targetNamespaces == null;
  }

  public void addedToEventManager() {
    registerWatch();
  }

  private void registerWatch() {
    CustomResourceOperationsImpl crClient = (CustomResourceOperationsImpl) client;
    if (isWatchAllNamespaces()) {
      crClient.inAnyNamespace().watch(this);
    } else if (targetNamespaces.length == 0) {
      client.watch(this);
    } else {
      for (String targetNamespace : targetNamespaces) {
        crClient.inNamespace(targetNamespace).watch(this);
        log.debug("Registered controller for namespace: {}", targetNamespace);
      }
    }
  }

  @Override
  public void eventReceived(Watcher.Action action, CustomResource customResource) {
    log.debug(
        "Event received for action: {}, resource: {}",
        action.name(),
        customResource.getMetadata().getName());

    resourceCache.cacheResource(
        customResource); // always store the latest event. Outside the sync block is intentional.
    if (action == Action.ERROR) {
      log.debug(
          "Skipping {} event for custom resource uid: {}, version: {}",
          action,
          getUID(customResource),
          getVersion(customResource));
      return;
    }

    if (!skipBecauseOfGenerations(customResource)) {
      eventHandler.handleEvent(new CustomResourceEvent(action, customResource, this));
      markLastGenerationProcessed(customResource);
    } else {
      log.debug(
          "Skipping event handling resource {} with version: {}",
          getUID(customResource),
          getVersion(customResource));
    }
  }

  private void markLastGenerationProcessed(CustomResource resource) {
    if (generationAware && resource.hasFinalizer(resourceFinalizer)) {
      lastGenerationProcessedSuccessfully.put(
          KubernetesResourceUtils.getUID(resource), resource.getMetadata().getGeneration());
    }
  }

  private boolean skipBecauseOfGenerations(CustomResource customResource) {
    if (!generationAware) {
      return false;
    }
    // if CR being deleted generation is naturally not changing, so we process all the events
    if (customResource.isMarkedForDeletion()) {
      return false;
    }
    if (!largerGenerationThenProcessedBefore(customResource)) {
      return true;
    }
    return false;
  }

  public boolean largerGenerationThenProcessedBefore(CustomResource resource) {
    Long lastGeneration = lastGenerationProcessedSuccessfully.get(resource.getMetadata().getUid());
    if (lastGeneration == null) {
      return true;
    } else {
      return resource.getMetadata().getGeneration() > lastGeneration;
    }
  }

  @Override
  public void eventSourceDeRegisteredForResource(String customResourceUid) {
    lastGenerationProcessedSuccessfully.remove(customResourceUid);
  }

  @Override
  public void onClose(WatcherException e) {
    if (e == null) {
      return;
    }
    if (e.isHttpGone()) {
      log.warn("Received error for watch, will try to reconnect.", e);
      registerWatch();
    } else {
      // Note that this should not happen normally, since fabric8 client handles reconnect.
      // In case it tries to reconnect this method is not called.
      log.error("Unexpected error happened with watch. Will exit.", e);
      System.exit(1);
    }
  }
}
