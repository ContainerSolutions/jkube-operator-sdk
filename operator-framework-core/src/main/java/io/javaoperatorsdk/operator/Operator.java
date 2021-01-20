package io.javaoperatorsdk.operator;

import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.config.ConfigurationService;
import io.javaoperatorsdk.operator.processing.CustomResourceCache;
import io.javaoperatorsdk.operator.processing.DefaultEventHandler;
import io.javaoperatorsdk.operator.processing.EventDispatcher;
import io.javaoperatorsdk.operator.processing.event.DefaultEventSourceManager;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;
import io.javaoperatorsdk.operator.processing.event.internal.CustomResourceEventSource;
import io.javaoperatorsdk.operator.processing.retry.GenericRetry;
import io.javaoperatorsdk.operator.processing.retry.Retry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("rawtypes")
public class Operator {

  private static final Logger log = LoggerFactory.getLogger(Operator.class);
  private final KubernetesClient k8sClient;
  private final ConfigurationService configurationService;
  private List<EventSourceManager> eventSourceManagers = new ArrayList<>();

  public Operator(KubernetesClient k8sClient, ConfigurationService configurationService) {
    this.k8sClient = k8sClient;
    this.configurationService = configurationService;
  }

  public <R extends CustomResource> void register(ResourceController<R> controller)
      throws OperatorException {
    final var configuration = configurationService.getConfigurationFor(controller);
    if (configuration == null) {
      log.warn(
          "Skipping registration of {} controller named {} because its configuration cannot be found.\n"
              + "Known controllers are: {}",
          controller.getClass().getCanonicalName(),
          ControllerUtils.getNameFor(controller),
          configurationService.getKnownControllerNames());
    } else {
      final var retry = GenericRetry.fromConfiguration(configuration.getRetryConfiguration());
      final var targetNamespaces = configuration.getNamespaces().toArray(new String[] {});
      registerController(controller, configuration.watchAllNamespaces(), retry, targetNamespaces);
    }
  }

  public <R extends CustomResource> void registerControllerForAllNamespaces(
      ResourceController<R> controller, Retry retry) throws OperatorException {
    registerController(controller, true, retry);
  }

  public <R extends CustomResource> void registerControllerForAllNamespaces(
      ResourceController<R> controller) throws OperatorException {
    registerController(controller, true, null);
  }

  public <R extends CustomResource> void registerController(
      ResourceController<R> controller, Retry retry, String... targetNamespaces)
      throws OperatorException {
    registerController(controller, false, retry, targetNamespaces);
  }

  public <R extends CustomResource> void registerController(
      ResourceController<R> controller, String... targetNamespaces) throws OperatorException {
    registerController(controller, false, null, targetNamespaces);
  }

  public void close() {
    eventSourceManagers.stream().forEach(EventSourceManager::close);
  }

  @SuppressWarnings("rawtypes")
  private <R extends CustomResource> void registerController(
      ResourceController<R> controller,
      boolean watchAllNamespaces,
      Retry retry,
      String... targetNamespaces)
      throws OperatorException {
    final var configuration = configurationService.getConfigurationFor(controller);
    Class<R> resClass = configuration.getCustomResourceClass();
    String finalizer = configuration.getFinalizer();
    MixedOperation client = k8sClient.customResources(resClass);
    EventDispatcher eventDispatcher =
        new EventDispatcher(
            controller, finalizer, new EventDispatcher.CustomResourceFacade(client));

    CustomResourceCache customResourceCache = new CustomResourceCache();
    DefaultEventHandler defaultEventHandler =
        new DefaultEventHandler(
            customResourceCache, eventDispatcher, controller.getClass().getName(), retry);
    DefaultEventSourceManager eventSourceManager =
        new DefaultEventSourceManager(defaultEventHandler, retry != null);
    defaultEventHandler.setEventSourceManager(eventSourceManager);
    eventDispatcher.setEventSourceManager(eventSourceManager);

    controller.init(eventSourceManager);
    CustomResourceEventSource customResourceEventSource =
        createCustomResourceEventSource(
            client,
            customResourceCache,
            watchAllNamespaces,
            targetNamespaces,
            defaultEventHandler,
            configuration.isGenerationAware(),
            finalizer);
    eventSourceManager.registerCustomResourceEventSource(customResourceEventSource);

    eventSourceManagers.add(eventSourceManager);

    log.info(
        "Registered Controller: '{}' for CRD: '{}' for namespaces: {}",
        controller.getClass().getSimpleName(),
        resClass,
        targetNamespaces.length == 0
            ? "[all/client namespace]"
            : Arrays.toString(targetNamespaces));
  }

  private CustomResourceEventSource createCustomResourceEventSource(
      MixedOperation client,
      CustomResourceCache customResourceCache,
      boolean watchAllNamespaces,
      String[] targetNamespaces,
      DefaultEventHandler defaultEventHandler,
      boolean generationAware,
      String finalizer) {
    CustomResourceEventSource customResourceEventSource =
        watchAllNamespaces
            ? CustomResourceEventSource.customResourceEventSourceForAllNamespaces(
                customResourceCache, client, generationAware, finalizer)
            : CustomResourceEventSource.customResourceEventSourceForTargetNamespaces(
                customResourceCache, client, targetNamespaces, generationAware, finalizer);

    customResourceEventSource.setEventHandler(defaultEventHandler);

    return customResourceEventSource;
  }
}
