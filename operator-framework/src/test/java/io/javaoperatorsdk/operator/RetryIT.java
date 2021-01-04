package io.javaoperatorsdk.operator;

import static io.javaoperatorsdk.operator.IntegrationTestSupport.TEST_NAMESPACE;
import static org.assertj.core.api.Assertions.assertThat;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.processing.retry.GenericRetry;
import io.javaoperatorsdk.operator.processing.retry.Retry;
import io.javaoperatorsdk.operator.sample.retry.RetryTestCustomResource;
import io.javaoperatorsdk.operator.sample.retry.RetryTestCustomResourceController;
import io.javaoperatorsdk.operator.sample.retry.RetryTestCustomResourceSpec;
import io.javaoperatorsdk.operator.sample.retry.RetryTestCustomResourceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RetryIT {

  public static final int RETRY_INTERVAL = 150;
  private IntegrationTestSupport integrationTestSupport = new IntegrationTestSupport();

  @BeforeEach
  public void initAndCleanup() {
    Retry retry =
        new GenericRetry().setInitialInterval(RETRY_INTERVAL).withLinearRetry().setMaxAttempts(5);
    KubernetesClient k8sClient = new DefaultKubernetesClient();
    integrationTestSupport.initialize(
        k8sClient, new RetryTestCustomResourceController(), "retry-test-crd.yaml", retry);
    integrationTestSupport.cleanup();
  }

  @Test
  public void retryFailedExecution() {
    integrationTestSupport.teardownIfSuccess(
        () -> {
          RetryTestCustomResource resource = createTestCustomResource("1");
          integrationTestSupport.getCrOperations().inNamespace(TEST_NAMESPACE).create(resource);

          Thread.sleep(
              RETRY_INTERVAL * (RetryTestCustomResourceController.NUMBER_FAILED_EXECUTIONS + 2));

          assertThat(integrationTestSupport.numberOfControllerExecutions())
              .isGreaterThanOrEqualTo(
                  RetryTestCustomResourceController.NUMBER_FAILED_EXECUTIONS + 1);

          RetryTestCustomResource finalResource =
              (RetryTestCustomResource)
                  integrationTestSupport
                      .getCrOperations()
                      .inNamespace(TEST_NAMESPACE)
                      .withName(resource.getMetadata().getName())
                      .get();
          assertThat(finalResource.getStatus().getState())
              .isEqualTo(RetryTestCustomResourceStatus.State.SUCCESS);
        });
  }

  public RetryTestCustomResource createTestCustomResource(String id) {
    RetryTestCustomResource resource = new RetryTestCustomResource();
    resource.setMetadata(
        new ObjectMetaBuilder()
            .withName("retrysource-" + id)
            .withNamespace(TEST_NAMESPACE)
            .withFinalizers(RetryTestCustomResourceController.FINALIZER_NAME)
            .build());
    resource.setKind("retrysample");
    resource.setSpec(new RetryTestCustomResourceSpec());
    resource.getSpec().setValue(id);
    return resource;
  }
}
