package io.javaoperatorsdk.operator.sample;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.StandaloneOperator;
import org.springframework.stereotype.Component;

/** This component just showcases what beans are registered. */
@Component
public class SampleComponent {

  private final StandaloneOperator operator;

  private final KubernetesClient kubernetesClient;

  private final CustomServiceController customServiceController;

  public SampleComponent(
      StandaloneOperator operator,
      KubernetesClient kubernetesClient,
      CustomServiceController customServiceController) {
    this.operator = operator;
    this.kubernetesClient = kubernetesClient;
    this.customServiceController = customServiceController;
  }
}
