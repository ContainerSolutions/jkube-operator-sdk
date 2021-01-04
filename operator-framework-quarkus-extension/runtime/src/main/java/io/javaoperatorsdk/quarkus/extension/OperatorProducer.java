package io.javaoperatorsdk.quarkus.extension;

import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.Operator;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.quarkus.arc.DefaultBean;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class OperatorProducer {

  @Inject Instance<ResourceController<? extends CustomResource>> controllers;

  @Produces
  @DefaultBean
  @Singleton
  Operator operator(KubernetesClient client, QuarkusConfigurationService configuration) {
    final var operator = new QuarkusOperator(client, configuration);
    controllers.stream().forEach(operator::register);
    return operator;
  }
}
