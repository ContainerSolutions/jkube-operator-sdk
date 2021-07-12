package io.javaoperatorsdk.operator.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Interface to be implemented by user-provided controller classes.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Controller {

  /**
   * String representing no name provided.
   */
  String NULL = "";

  /**
   * String representing config setting for custom resource controllers to watch the currently
   * active namespace.
   */
  String WATCH_CURRENT_NAMESPACE = "JOSDK_WATCH_CURRENT";

  /**
   * String representing explicitly that no finalizer should be added to custom resources managed by
   * the controller.
   */
  String NO_FINALIZER = "JOSDK_NO_FINALIZER";

  /**
   * Gets the name of the controller.
   *
   * @return the name of the controller
   */
  String name() default NULL;

  /**
   * Optional, the name of the finalizer attached to custom resources managed by the controller. If
   * it is not provided one will be automatically generated. If the provided value is
   * {@link #NO_FINALIZER}, then no finalizer will be attached to custom resources.
   *
   * @return the finalizer name
   */
  String finalizerName() default NULL;

  /**
   * If true, will dispatch new event to the controller if
   * <a href=
   * https://kubernetes.io/docs/tasks/access-kubernetes-api/custom-resources/custom-resource-definitions/#status-subresource
   * >generation</a>
   * increased since the last processing, otherwise will process all events.
   *
   * @return whether the controller takes generation into account to process events
   */
  boolean generationAwareEventProcessing() default true;

  /**
   * Specified which namespaces this Controller monitors for custom resources events. If no
   * namespace is specified then the controller will monitor all namespaces by default.
   *
   * @return the list of namespaces this controller monitors
   */
  String[] namespaces() default {};
}
