package io.javaoperatorsdk.operator.sample;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller(crdName = "webapps.tomcatoperator.io")
public class WebappController implements ResourceController<Webapp> {

  private KubernetesClient kubernetesClient;

  private final Logger log = LoggerFactory.getLogger(getClass());

  public WebappController(KubernetesClient kubernetesClient) {
    this.kubernetesClient = kubernetesClient;
  }

  @Override
  public UpdateControl<Webapp> createOrUpdateResource(Webapp webapp, Context<Webapp> context) {
    if (Objects.equals(webapp.getSpec().getUrl(), webapp.getStatus().getDeployedArtifact())) {
      return UpdateControl.noUpdate();
    }

    String fileName = fileNameFromWebapp(webapp);
    String[] command = new String[] {"wget", "-O", "/data/" + fileName, webapp.getSpec().getUrl()};

    executeCommandInAllPods(kubernetesClient, webapp, command);

    webapp.getStatus().setDeployedArtifact(webapp.getSpec().getUrl());
    return UpdateControl.updateStatusSubResource(webapp);
  }

  @Override
  public DeleteControl deleteResource(Webapp webapp, Context<Webapp> context) {
    String fileName = fileNameFromWebapp(webapp);
    String[] command = new String[] {"rm", "/data/" + fileName};
    executeCommandInAllPods(kubernetesClient, webapp, command);
    return DeleteControl.DEFAULT_DELETE;
  }

  private void executeCommandInAllPods(
      KubernetesClient kubernetesClient, Webapp webapp, String[] command) {
    Deployment deployment =
        kubernetesClient
            .apps()
            .deployments()
            .inNamespace(webapp.getMetadata().getNamespace())
            .withName(webapp.getSpec().getTomcat())
            .get();

    if (deployment != null) {
      List<Pod> pods =
          kubernetesClient
              .pods()
              .inNamespace(webapp.getMetadata().getNamespace())
              .withLabels(deployment.getSpec().getSelector().getMatchLabels())
              .list()
              .getItems();
      for (Pod pod : pods) {
        log.info(
            "Executing command {} in Pod {}",
            String.join(" ", command),
            pod.getMetadata().getName());
        kubernetesClient
            .pods()
            .inNamespace(deployment.getMetadata().getNamespace())
            .withName(pod.getMetadata().getName())
            .inContainer("war-downloader")
            .writingOutput(new ByteArrayOutputStream())
            .writingError(new ByteArrayOutputStream())
            .exec(command);
      }
    }
  }

  private String fileNameFromWebapp(Webapp webapp) {
    try {
      if (webapp.getSpec().getContextPath() == null) {
        Pattern regexpPattern = Pattern.compile("([^\\/]+$)");
        Matcher regexpMatcher = regexpPattern.matcher(webapp.getSpec().getUrl());
        regexpMatcher.find();
        return regexpMatcher.group();
      } else {
        return webapp.getSpec().getContextPath() + ".war";
      }
    } catch (RuntimeException ex) {
      log.error("Failed to parse file name from URL {}", webapp.getSpec().getUrl());
      throw ex;
    }
  }
}
