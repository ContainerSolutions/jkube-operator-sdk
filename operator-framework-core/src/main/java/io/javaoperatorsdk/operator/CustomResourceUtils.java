package io.javaoperatorsdk.operator;

import io.fabric8.kubernetes.api.model.Cluster;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import java.util.Arrays;

/**
 * Static class to group together utility methods relating to custom resources.
 */
public abstract class CustomResourceUtils {

  /**
   * Applies internal validations that may not be handled by the fabric8 client.
   *
   * @param resClass Custom Resource to validate
   * @param crd CRD for the Custom Resource
   * @throws OperatorException when the Custom Resource has validation error
   */
  public static void assertCustomResource(Class<?> resClass, CustomResourceDefinition crd) {
    var namespaced = Arrays.asList(resClass.getInterfaces()).contains(Namespaced.class);

    if (!namespaced && Namespaced.class.getSimpleName().equals(crd.getSpec().getScope())) {
      throw new OperatorException(
          "Custom resource '"
              + resClass.getName()
              + "' must implement '"
              + Namespaced.class.getName()
              + "' since CRD '"
              + crd.getMetadata().getName()
              + "' is scoped as 'Namespaced'");
    } else if (namespaced && Cluster.class.getSimpleName().equals(crd.getSpec().getScope())) {
      throw new OperatorException(
          "Custom resource '"
              + resClass.getName()
              + "' must not implement '"
              + Namespaced.class.getName()
              + "' since CRD '"
              + crd.getMetadata().getName()
              + "' is scoped as 'Cluster'");
    }
  }
}
