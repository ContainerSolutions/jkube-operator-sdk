package io.javaoperatorsdk.operator;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Version;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.config.ConfigurationService;
import io.javaoperatorsdk.operator.api.config.ControllerConfiguration;
import io.javaoperatorsdk.operator.processing.CustomResourceCache;
import io.javaoperatorsdk.operator.processing.DefaultEventHandler;
import io.javaoperatorsdk.operator.processing.EventDispatcher;
import io.javaoperatorsdk.operator.processing.cache.CaffeinCacheAdaptor;
import io.javaoperatorsdk.operator.processing.cache.PassThroughResourceCache;
import io.javaoperatorsdk.operator.processing.cache.ResourceCache;
import io.javaoperatorsdk.operator.processing.event.DefaultEventSourceManager;
import io.javaoperatorsdk.operator.processing.event.internal.CustomResourceEventSource;
import io.javaoperatorsdk.operator.processing.retry.GenericRetry;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("rawtypes")
public class Operator {

  private static final Logger log = LoggerFactory.getLogger(Operator.class);
  private final KubernetesClient k8sClient;
  private final ConfigurationService configurationService;
  private final ObjectMapper objectMapper;
  private final ResourceCache resourceCache;

  public Operator(KubernetesClient k8sClient, ConfigurationService configurationService) {
    this(k8sClient, configurationService, new ObjectMapper(), new CaffeinCacheAdaptor());
  }

  public Operator(
      KubernetesClient k8sClient,
      ConfigurationService configurationService,
      ObjectMapper objectMapper,
      ResourceCache resourceCache) {
    this.k8sClient = k8sClient;
    this.configurationService = configurationService;
    this.objectMapper = objectMapper;
    this.resourceCache = resourceCache;
  }

  /**
   * Finishes the operator startup process. This is mostly used in injection-aware applications
   * where there is no obvious entrypoint to the application which can trigger the injection process
   * and start the cluster monitoring processes.
   */
  public void start() {
    final var version = configurationService.getVersion();
    log.info(
        "Operator {} (commit: {}) built on {} starting...",
        version.getSdkVersion(),
        version.getCommit(),
        version.getBuiltTime());
    log.info("Client version: {}", Version.clientVersion());
    try {
      final var k8sVersion = k8sClient.getVersion();
      if (k8sVersion != null) {
        log.info("Server version: {}.{}", k8sVersion.getMajor(), k8sVersion.getMinor());
      }
    } catch (Exception e) {
      log.error("Error retrieving the server version. Exiting!", e);
      System.exit(1);
    }
  }

  /**
   * Registers the specified controller with this operator.
   *
   * @param controller the controller to register
   * @param <R> the {@code CustomResource} type associated with the controller
   * @throws OperatorException if a problem occurred during the registration process
   */
  public <R extends CustomResource> void register(ResourceController<R> controller)
      throws OperatorException {
    register(controller, null);
  }

  /**
   * Registers the specified controller with this operator, overriding its default configuration by
   * the specified one (usually created via {@link
   * io.javaoperatorsdk.operator.api.config.ControllerConfigurationOverrider#override(ControllerConfiguration)},
   * passing it the controller's original configuration.
   *
   * @param controller the controller to register
   * @param configuration the configuration with which we want to register the controller, if {@code
   *     null}, the controller's original configuration is used
   * @param <R> the {@code CustomResource} type associated with the controller
   * @throws OperatorException if a problem occurred during the registration process
   */
  public <R extends CustomResource> void register(
      ResourceController<R> controller, ControllerConfiguration<R> configuration)
      throws OperatorException {
    final var existing = configurationService.getConfigurationFor(controller);
    if (existing == null) {
      log.warn(
          "Skipping registration of {} controller named {} because its configuration cannot be found.\n"
              + "Known controllers are: {}",
          controller.getClass().getCanonicalName(),
          ControllerUtils.getNameFor(controller),
          configurationService.getKnownControllerNames());
    } else {
      if (configuration == null) {
        configuration = existing;
      }

      final var retry = GenericRetry.fromConfiguration(configuration.getRetryConfiguration());

      // check if we only want to watch the current namespace
      var targetNamespaces = configuration.getNamespaces().toArray(new String[] {});
      if (configuration.watchCurrentNamespace()) {
        targetNamespaces =
            new String[] {configurationService.getClientConfiguration().getNamespace()};
      }

      Class<R> resClass = configuration.getCustomResourceClass();
      String finalizer = configuration.getFinalizer();

      final String controllerName = configuration.getName();

      // check that the custom resource is known by the cluster if configured that way
      final CustomResourceDefinition crd;
      if (configurationService.checkCRDAndValidateLocalModel()) {
        final var crdName = configuration.getCRDName();
        crd = k8sClient.apiextensions().v1().customResourceDefinitions().withName(crdName).get();
        if (crd == null) {
          throw new OperatorException(
              "'"
                  + crdName
                  + "' CRD was not found on the cluster, controller "
                  + controllerName
                  + " cannot be registered");
        }

        // Apply validations that are not handled by fabric8
        CustomResourceUtils.assertCustomResource(resClass, crd);
      }

      final var client = k8sClient.customResources(resClass);
      EventDispatcher<R> dispatcher = new EventDispatcher<>(controller, finalizer, client);

      PassThroughResourceCache passThroughResourceCache = new PassThroughResourceCache(resourceCache,client,objectMapper);
      DefaultEventHandler defaultEventHandler =
          new DefaultEventHandler(passThroughResourceCache, dispatcher, controllerName, retry);
      DefaultEventSourceManager eventSourceManager =
          new DefaultEventSourceManager(defaultEventHandler, retry != null);
      defaultEventHandler.setEventSourceManager(eventSourceManager);
      dispatcher.setEventSourceManager(eventSourceManager);

      controller.init(eventSourceManager);
      final boolean watchAllNamespaces = configuration.watchAllNamespaces();
      CustomResourceEventSource customResourceEventSource =
          createCustomResourceEventSource(
              client,
              passThroughResourceCache,
              watchAllNamespaces,
              targetNamespaces,
              defaultEventHandler,
              configuration.isGenerationAware(),
              finalizer);
      eventSourceManager.registerCustomResourceEventSource(customResourceEventSource);

      log.info(
          "Registered Controller: '{}' for CRD: '{}' for namespace(s): {}",
          controllerName,
          resClass,
          watchAllNamespaces ? "[all namespaces]" : Arrays.toString(targetNamespaces));
    }
  }

  private CustomResourceEventSource createCustomResourceEventSource(
      MixedOperation client,
      PassThroughResourceCache customResourceCache,
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
