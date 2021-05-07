package io.javaoperatorsdk.operator.processing.event.internal;

import static io.javaoperatorsdk.operator.processing.KubernetesResourceUtils.getUID;
import static io.javaoperatorsdk.operator.processing.KubernetesResourceUtils.getVersion;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.internal.CustomResourceOperationsImpl;
import io.javaoperatorsdk.operator.api.config.ControllerConfiguration;
import io.javaoperatorsdk.operator.processing.KubernetesResourceUtils;
import io.javaoperatorsdk.operator.processing.event.AbstractEventSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This is a special case since is not bound to a single custom resource */
public class CustomResourceEventSource<T extends CustomResource<?, ?>> extends AbstractEventSource
    implements Watcher<T> {

  private static final Logger log = LoggerFactory.getLogger(CustomResourceEventSource.class);

  private final CustomResourceOperationsImpl<T, KubernetesResourceList<T>> client;
  private final Set<String> targetNamespaces;
  private final boolean generationAware;
  private final String resourceFinalizer;
  private final Map<String, Long> lastGenerationProcessedSuccessfully = new ConcurrentHashMap<>();
  private final List<Watch> watches;
  private final String resClass;

  public CustomResourceEventSource(
      MixedOperation<T, KubernetesResourceList<T>, Resource<T>> client,
      ControllerConfiguration<T> configuration) {
    this(
        client,
        configuration.getEffectiveNamespaces(),
        configuration.isGenerationAware(),
        configuration.getFinalizer(),
        configuration.getCustomResourceClass());
  }

  CustomResourceEventSource(
      MixedOperation<T, KubernetesResourceList<T>, Resource<T>> client,
      Set<String> targetNamespaces,
      boolean generationAware,
      String resourceFinalizer,
      Class<T> resClass) {
    this.client = (CustomResourceOperationsImpl<T, KubernetesResourceList<T>>) client;
    this.targetNamespaces = targetNamespaces;
    this.generationAware = generationAware;
    this.resourceFinalizer = resourceFinalizer;
    this.watches = new ArrayList<>();
    this.resClass = resClass.getName();
  }

  @Override
  public void start() {
    if (ControllerConfiguration.allNamespacesWatched(targetNamespaces)) {
      var w = client.inAnyNamespace().watch(this);
      watches.add(w);
      log.debug("Registered controller {} -> {} for any namespace", resClass, w);
    } else {
      targetNamespaces.forEach(
          ns -> {
            var w = client.inNamespace(ns).watch(this);
            watches.add(w);
            log.debug("Registered controller {} -> {} for namespace: {}", resClass, w, ns);
          });
    }
  }

  @Override
  public void close() {
    for (Watch watch : this.watches) {
      try {
        log.debug("Closing watch {} -> {}", resClass, watch);
        watch.close();
      } catch (Exception e) {
        log.warn("Error closing watcher {} -> {}", resClass, watch, e);
      }
    }
  }

  @Override
  public void eventReceived(Watcher.Action action, T customResource) {
    log.debug(
        "Event received for action: {}, resource: {}",
        action.name(),
        customResource.getMetadata().getName());

    if (action == Action.ERROR) {
      log.debug(
          "Skipping {} event for custom resource uid: {}, version: {}",
          action,
          getUID(customResource),
          getVersion(customResource));
      return;
    }

    if (!skipBecauseOfGeneration(customResource)) {
      eventHandler.handleEvent(new CustomResourceEvent(action, customResource, this));
      markLastGenerationProcessed(customResource);
    } else {
      log.debug(
          "Skipping event handling resource {} with version: {}",
          getUID(customResource),
          getVersion(customResource));
    }
  }

  private void markLastGenerationProcessed(T resource) {
    if (generationAware && resource.hasFinalizer(resourceFinalizer)) {
      lastGenerationProcessedSuccessfully.put(
          KubernetesResourceUtils.getUID(resource), resource.getMetadata().getGeneration());
    }
  }

  private boolean skipBecauseOfGeneration(T customResource) {
    if (!generationAware) {
      return false;
    }
    // if CR being deleted generation is naturally not changing, so we process all the events
    if (customResource.isMarkedForDeletion()) {
      return false;
    }
    return !hasGenerationAlreadyBeenProcessed(customResource);
  }

  private boolean hasGenerationAlreadyBeenProcessed(T resource) {
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
      try {
        close();
        start();
      } catch (Throwable ex) {
        log.error("Unexpected error happened with watch reconnect. Will exit.", e);
        System.exit(1);
      }
    } else {
      // Note that this should not happen normally, since fabric8 client handles reconnect.
      // In case it tries to reconnect this method is not called.
      log.error("Unexpected error happened with watch. Will exit.", e);
      System.exit(1);
    }
  }
}
