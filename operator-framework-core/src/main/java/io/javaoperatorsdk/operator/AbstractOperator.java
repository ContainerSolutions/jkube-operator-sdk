package io.javaoperatorsdk.operator;

import io.fabric8.kubernetes.api.model.apiextensions.v1beta1.CustomResourceDefinition;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.CustomResourceDoneable;
import io.fabric8.kubernetes.client.CustomResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.dsl.internal.CustomResourceOperationsImpl;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.config.ConfigurationService;
import io.javaoperatorsdk.operator.processing.CustomResourceCache;
import io.javaoperatorsdk.operator.processing.DefaultEventHandler;
import io.javaoperatorsdk.operator.processing.EventDispatcher;
import io.javaoperatorsdk.operator.processing.event.DefaultEventSourceManager;
import io.javaoperatorsdk.operator.processing.event.internal.CustomResourceEventSource;
import io.javaoperatorsdk.operator.processing.retry.GenericRetry;
import io.javaoperatorsdk.operator.processing.retry.Retry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("rawtypes")
public abstract class AbstractOperator implements Operator {

  private static final Logger log = LoggerFactory.getLogger(AbstractOperator.class);
  private final KubernetesClient k8sClient;
  private final ConfigurationService configurationService;
  private Map<Class<? extends CustomResource>, CustomResourceOperationsImpl> customResourceClients =
      new HashMap<>();

  public AbstractOperator(KubernetesClient k8sClient, ConfigurationService configurationService) {
    this.k8sClient = k8sClient;
    this.configurationService = configurationService;
  }

  @Override
  public <R extends CustomResource> void register(ResourceController<R> controller)
      throws OperatorException {
    final var configuration = configurationService.getConfigurationFor(controller);
    final var retry = GenericRetry.fromConfiguration(configuration.getRetryConfiguration());
    final var targetNamespaces = configuration.getNamespaces().toArray(new String[] {});
    registerController(controller, configuration.watchAllNamespaces(), retry, targetNamespaces);
  }

  @Override
  public <R extends CustomResource> void registerControllerForAllNamespaces(
      ResourceController<R> controller, Retry retry) throws OperatorException {
    registerController(controller, true, retry);
  }

  @Override
  public <R extends CustomResource> void registerControllerForAllNamespaces(
      ResourceController<R> controller) throws OperatorException {
    registerController(controller, true, null);
  }

  @Override
  public <R extends CustomResource> void registerController(
      ResourceController<R> controller, Retry retry, String... targetNamespaces)
      throws OperatorException {
    registerController(controller, false, retry, targetNamespaces);
  }

  @Override
  public <R extends CustomResource> void registerController(
      ResourceController<R> controller, String... targetNamespaces) throws OperatorException {
    registerController(controller, false, null, targetNamespaces);
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
    CustomResourceDefinitionContext crd = getCustomResourceDefinitionForController(controller);
    KubernetesDeserializer.registerCustomKind(crd.getVersion(), crd.getKind(), resClass);
    String finalizer = configuration.getFinalizer();
    MixedOperation client =
        k8sClient.customResources(
            crd, resClass, CustomResourceList.class, configuration.getDoneableClass());
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

    customResourceClients.put(resClass, (CustomResourceOperationsImpl) client);

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

  private CustomResourceDefinitionContext getCustomResourceDefinitionForController(
      ResourceController controller) {
    final var crdName = configurationService.getConfigurationFor(controller).getCRDName();
    CustomResourceDefinition customResourceDefinition =
        k8sClient.customResourceDefinitions().withName(crdName).get();
    if (customResourceDefinition == null) {
      throw new OperatorException("Cannot find Custom Resource Definition with name: " + crdName);
    }
    CustomResourceDefinitionContext context =
        CustomResourceDefinitionContext.fromCrd(customResourceDefinition);
    return context;
  }

  @Override
  public Map<Class<? extends CustomResource>, CustomResourceOperationsImpl>
      getCustomResourceClients() {
    return customResourceClients;
  }

  @Override
  public <
          T extends CustomResource,
          L extends CustomResourceList<T>,
          D extends CustomResourceDoneable<T>>
      CustomResourceOperationsImpl<T, L, D> getCustomResourceClients(Class<T> customResourceClass) {
    return customResourceClients.get(customResourceClass);
  }
}
