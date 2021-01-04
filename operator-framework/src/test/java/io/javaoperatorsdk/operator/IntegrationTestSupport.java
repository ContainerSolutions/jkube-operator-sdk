package io.javaoperatorsdk.operator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1beta1.CustomResourceDefinition;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.CustomResourceDoneable;
import io.fabric8.kubernetes.client.CustomResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.config.runtime.DefaultConfigurationService;
import io.javaoperatorsdk.operator.processing.retry.Retry;
import io.javaoperatorsdk.operator.sample.simple.TestCustomResource;
import io.javaoperatorsdk.operator.sample.simple.TestCustomResourceSpec;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IntegrationTestSupport {

  public static final String TEST_NAMESPACE = "java-operator-sdk-int-test";
  public static final String TEST_CUSTOM_RESOURCE_PREFIX = "test-custom-resource-";
  private static final Logger log = LoggerFactory.getLogger(IntegrationTestSupport.class);
  private KubernetesClient k8sClient;
  private MixedOperation<
          CustomResource,
          CustomResourceList,
          CustomResourceDoneable,
          Resource<CustomResource, CustomResourceDoneable>>
      crOperations;
  private Operator operator;
  private ResourceController controller;

  public void initialize(
      KubernetesClient k8sClient, ResourceController controller, String crdPath) {
    initialize(k8sClient, controller, crdPath, null);
  }

  public void initialize(
      KubernetesClient k8sClient, ResourceController controller, String crdPath, Retry retry) {
    log.info("Initializing integration test in namespace {}", TEST_NAMESPACE);
    this.k8sClient = k8sClient;
    CustomResourceDefinition crd = loadCRDAndApplyToCluster(crdPath);
    CustomResourceDefinitionContext crdContext = CustomResourceDefinitionContext.fromCrd(crd);
    this.controller = controller;

    final var configurationService = DefaultConfigurationService.instance();

    final var config = configurationService.getConfigurationFor(controller);
    Class doneableClass = config.getDoneableClass();
    Class customResourceClass = config.getCustomResourceClass();
    crOperations =
        k8sClient.customResources(
            crdContext, customResourceClass, CustomResourceList.class, doneableClass);

    if (k8sClient.namespaces().withName(TEST_NAMESPACE).get() == null) {
      k8sClient
          .namespaces()
          .create(
              new NamespaceBuilder()
                  .withMetadata(new ObjectMetaBuilder().withName(TEST_NAMESPACE).build())
                  .build());
    }
    operator = new StandaloneOperator(k8sClient, configurationService);
    operator.registerController(controller, retry, TEST_NAMESPACE);
    log.info("Operator is running with {}", controller.getClass().getCanonicalName());
  }

  public CustomResourceDefinition loadCRDAndApplyToCluster(String classPathYaml) {
    CustomResourceDefinition crd = loadYaml(CustomResourceDefinition.class, classPathYaml);
    k8sClient.customResourceDefinitions().createOrReplace(crd);
    return crd;
  }

  public void cleanup() {
    log.info("Cleaning up namespace {}", TEST_NAMESPACE);

    // we depend on the actual operator from the startup to handle the finalizers and clean up
    // resources from previous test runs
    crOperations.inNamespace(TEST_NAMESPACE).delete(crOperations.list().getItems());

    await("all CRs cleaned up")
        .atMost(60, TimeUnit.SECONDS)
        .untilAsserted(
            () -> assertThat(crOperations.inNamespace(TEST_NAMESPACE).list().getItems()).isEmpty());

    k8sClient
        .configMaps()
        .inNamespace(TEST_NAMESPACE)
        .withLabel("managedBy", controller.getClass().getSimpleName())
        .delete();

    await("all config maps cleaned up")
        .atMost(60, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              assertThat(
                  k8sClient
                      .configMaps()
                      .inNamespace(TEST_NAMESPACE)
                      .withLabel("managedBy", controller.getClass().getSimpleName())
                      .list()
                      .getItems()
                      .isEmpty());
            });

    log.info("Cleaned up namespace " + TEST_NAMESPACE);
  }

  /**
   * Use this method to execute the cleanup of the integration test namespace only in case the test
   * was successful. This is useful to keep the Kubernetes resources around to debug a failed test
   * run. Unfortunately I couldn't make this work with standard JUnit methods as the @AfterAll
   * method doesn't know if the tests succeeded or not.
   *
   * @param test The code of the actual test.
   * @throws Exception if the test threw an exception.
   */
  public void teardownIfSuccess(TestRun test) {
    try {
      test.run();

      log.info("Deleting namespace {} and stopping operator", TEST_NAMESPACE);
      Namespace namespace = k8sClient.namespaces().withName(TEST_NAMESPACE).get();
      if (namespace.getStatus().getPhase().equals("Active")) {
        k8sClient.namespaces().withName(TEST_NAMESPACE).delete();
      }
      await("namespace deleted")
          .atMost(45, TimeUnit.SECONDS)
          .until(() -> k8sClient.namespaces().withName(TEST_NAMESPACE).get() == null);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    } finally {
      k8sClient.close();
    }
  }

  public int numberOfControllerExecutions() {
    return ((TestExecutionInfoProvider) controller).getNumberOfExecutions();
  }

  private <T> T loadYaml(Class<T> clazz, String yaml) {
    try (InputStream is = getClass().getResourceAsStream(yaml)) {
      return Serialization.unmarshal(is, clazz);
    } catch (IOException ex) {
      throw new IllegalStateException("Cannot find yaml on classpath: " + yaml);
    }
  }

  public TestCustomResource createTestCustomResource(String id) {
    TestCustomResource resource = new TestCustomResource();
    resource.setMetadata(
        new ObjectMetaBuilder()
            .withName(TEST_CUSTOM_RESOURCE_PREFIX + id)
            .withNamespace(TEST_NAMESPACE)
            .build());
    resource.setKind("CustomService");
    resource.setSpec(new TestCustomResourceSpec());
    resource.getSpec().setConfigMapName("test-config-map-" + id);
    resource.getSpec().setKey("test-key");
    resource.getSpec().setValue(id);
    return resource;
  }

  public KubernetesClient getK8sClient() {
    return k8sClient;
  }

  public MixedOperation<
          CustomResource,
          CustomResourceList,
          CustomResourceDoneable,
          Resource<CustomResource, CustomResourceDoneable>>
      getCrOperations() {
    return crOperations;
  }

  public CustomResource getCustomResource(String name) {
    return getCrOperations().inNamespace(TEST_NAMESPACE).withName(name).get();
  }

  public Operator getOperator() {
    return operator;
  }

  public interface TestRun {

    void run() throws Exception;
  }
}
