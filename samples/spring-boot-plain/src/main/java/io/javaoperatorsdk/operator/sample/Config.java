package io.javaoperatorsdk.operator.sample;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.StandaloneOperator;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.config.runtime.DefaultConfigurationService;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {

  @Bean
  public KubernetesClient kubernetesClient() {
    return new DefaultKubernetesClient();
  }

  @Bean
  public CustomServiceController customServiceController(KubernetesClient client) {
    return new CustomServiceController(client);
  }

  //  Register all controller beans
  @Bean
  public StandaloneOperator operator(
      KubernetesClient client, List<ResourceController> controllers) {
    StandaloneOperator operator =
        new StandaloneOperator(client, DefaultConfigurationService.instance());
    controllers.forEach(c -> operator.registerControllerForAllNamespaces(c));
    return operator;
  }
}
