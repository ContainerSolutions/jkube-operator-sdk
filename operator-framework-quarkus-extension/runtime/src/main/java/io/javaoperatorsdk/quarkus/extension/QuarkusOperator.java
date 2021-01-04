package io.javaoperatorsdk.quarkus.extension;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.AbstractOperator;

public class QuarkusOperator extends AbstractOperator {

  public QuarkusOperator(
      KubernetesClient k8sClient, QuarkusConfigurationService configurationService) {
    super(k8sClient, configurationService);
  }
}
