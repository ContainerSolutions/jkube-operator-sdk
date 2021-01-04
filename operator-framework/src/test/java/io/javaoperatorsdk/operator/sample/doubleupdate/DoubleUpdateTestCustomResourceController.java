package io.javaoperatorsdk.operator.sample.doubleupdate;

import io.javaoperatorsdk.operator.TestExecutionInfoProvider;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller(crdName = DoubleUpdateTestCustomResourceController.CRD_NAME)
public class DoubleUpdateTestCustomResourceController
    implements ResourceController<DoubleUpdateTestCustomResource>, TestExecutionInfoProvider {

  public static final String CRD_NAME = "doubleupdatesamples.sample.javaoperatorsdk";
  private static final Logger log =
      LoggerFactory.getLogger(DoubleUpdateTestCustomResourceController.class);
  public static final String TEST_ANNOTATION = "TestAnnotation";
  public static final String TEST_ANNOTATION_VALUE = "TestAnnotationValue";
  private final AtomicInteger numberOfExecutions = new AtomicInteger(0);

  @Override
  public DeleteControl deleteResource(
      DoubleUpdateTestCustomResource resource, Context<DoubleUpdateTestCustomResource> context) {
    return DeleteControl.DEFAULT_DELETE;
  }

  @Override
  public UpdateControl<DoubleUpdateTestCustomResource> createOrUpdateResource(
      DoubleUpdateTestCustomResource resource, Context<DoubleUpdateTestCustomResource> context) {
    numberOfExecutions.addAndGet(1);

    log.info("Value: " + resource.getSpec().getValue());

    resource.getMetadata().setAnnotations(new HashMap<>());
    resource.getMetadata().getAnnotations().put(TEST_ANNOTATION, TEST_ANNOTATION_VALUE);
    ensureStatusExists(resource);
    resource.getStatus().setState(DoubleUpdateTestCustomResourceStatus.State.SUCCESS);

    return UpdateControl.updateCustomResourceAndStatus(resource);
  }

  private void ensureStatusExists(DoubleUpdateTestCustomResource resource) {
    DoubleUpdateTestCustomResourceStatus status = resource.getStatus();
    if (status == null) {
      status = new DoubleUpdateTestCustomResourceStatus();
      resource.setStatus(status);
    }
  }

  public int getNumberOfExecutions() {
    return numberOfExecutions.get();
  }
}
