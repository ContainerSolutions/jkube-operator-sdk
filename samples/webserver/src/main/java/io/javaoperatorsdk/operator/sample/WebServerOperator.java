package io.javaoperatorsdk.operator.sample;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.Operator;
import io.javaoperatorsdk.operator.StandaloneOperator;
import io.javaoperatorsdk.operator.config.runtime.DefaultConfigurationService;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.takes.facets.fork.FkRegex;
import org.takes.facets.fork.TkFork;
import org.takes.http.Exit;
import org.takes.http.FtBasic;

public class WebServerOperator {

  private static final Logger log = LoggerFactory.getLogger(WebServerOperator.class);

  public static void main(String[] args) throws IOException {
    log.info("WebServer Operator starting!");

    Config config = new ConfigBuilder().withNamespace(null).build();
    KubernetesClient client = new DefaultKubernetesClient(config);
    Operator operator = new StandaloneOperator(client, DefaultConfigurationService.instance());
    operator.registerControllerForAllNamespaces(new WebServerController(client));

    new FtBasic(new TkFork(new FkRegex("/health", "ALL GOOD!")), 8080).start(Exit.NEVER);
  }
}
