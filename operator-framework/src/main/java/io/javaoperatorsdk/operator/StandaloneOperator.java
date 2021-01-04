package io.javaoperatorsdk.operator;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.config.ConfigurationService;

public class StandaloneOperator extends AbstractOperator {
  public StandaloneOperator(KubernetesClient k8sClient, ConfigurationService configurationService) {
    super(k8sClient, configurationService);
  }
}
