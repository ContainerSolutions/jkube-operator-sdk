package io.javaoperatorsdk.operator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.sample.simple.TestCustomResource;
import io.javaoperatorsdk.operator.sample.simple.TestCustomResourceController;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class ControllerExecutionIT {

  private IntegrationTestSupport integrationTestSupport = new IntegrationTestSupport();

  public void initAndCleanup(boolean controllerStatusUpdate) {
    KubernetesClient k8sClient = new DefaultKubernetesClient();
    integrationTestSupport.initialize(
        k8sClient,
        new TestCustomResourceController(k8sClient, controllerStatusUpdate),
        "test-crd.yaml");
    integrationTestSupport.cleanup();
  }

  @Test
  public void configMapGetsCreatedForTestCustomResource() {
    initAndCleanup(true);
    integrationTestSupport.teardownIfSuccess(
        () -> {
          TestCustomResource resource = TestUtils.testCustomResource();

          integrationTestSupport
              .getCrOperations()
              .inNamespace(IntegrationTestSupport.TEST_NAMESPACE)
              .create(resource);

          awaitResourcesCreatedOrUpdated();
          awaitStatusUpdated();
          assertThat(integrationTestSupport.numberOfControllerExecutions()).isEqualTo(2);
        });
  }

  @Test
  public void eventIsSkippedChangedOnMetadataOnlyUpdate() {
    initAndCleanup(false);
    integrationTestSupport.teardownIfSuccess(
        () -> {
          TestCustomResource resource = TestUtils.testCustomResource();

          integrationTestSupport
              .getCrOperations()
              .inNamespace(IntegrationTestSupport.TEST_NAMESPACE)
              .create(resource);

          awaitResourcesCreatedOrUpdated();
          assertThat(integrationTestSupport.numberOfControllerExecutions()).isEqualTo(1);
        });
  }

  void awaitResourcesCreatedOrUpdated() {
    await("config map created")
        .atMost(5, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              ConfigMap configMap =
                  integrationTestSupport
                      .getK8sClient()
                      .configMaps()
                      .inNamespace(IntegrationTestSupport.TEST_NAMESPACE)
                      .withName("test-config-map")
                      .get();
              assertThat(configMap).isNotNull();
              assertThat(configMap.getData().get("test-key")).isEqualTo("test-value");
            });
  }

  void awaitStatusUpdated() {
    awaitStatusUpdated(5);
  }

  void awaitStatusUpdated(int timeout) {
    await("cr status updated")
        .atMost(timeout, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              TestCustomResource cr =
                  (TestCustomResource)
                      integrationTestSupport
                          .getCrOperations()
                          .inNamespace(IntegrationTestSupport.TEST_NAMESPACE)
                          .withName(TestUtils.TEST_CUSTOM_RESOURCE_NAME)
                          .get();
              assertThat(cr).isNotNull();
              assertThat(cr.getStatus()).isNotNull();
              assertThat(cr.getStatus().getConfigMapStatus()).isEqualTo("ConfigMap Ready");
            });
  }
}
