package io.javaoperatorsdk.operator;

import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.ResourceController;
import java.util.Locale;

/**
 * Static class to group together utility methods relating to controllers.
 */
public class ControllerUtils {

  /**
   * Suffix to attach to the end of generated K8s finalizer names
   */
  private static final String FINALIZER_NAME_SUFFIX = "/finalizer";

  /**
   * Generates a default finalizer name for a provided controller name.
   * @param crdName the name of the controller
   * @return the finalizer name
   */
  public static String getDefaultFinalizerName(String crdName) {
    return crdName + FINALIZER_NAME_SUFFIX;
  }

  /**
   * Gets the canonical stringified name for a provided controller class.
   * @param controllerClass the controller class to be named
   * @return the name string
   */
  public static String getNameFor(Class<? extends ResourceController> controllerClass) {
    // if the controller annotation has a name attribute, use it
    final var annotation = controllerClass.getAnnotation(Controller.class);
    if (annotation != null) {
      final var name = annotation.name();
      if (!Controller.NULL.equals(name)) {
        return name;
      }
    }

    // otherwise, use the lower-cased full class name
    return getDefaultNameFor(controllerClass);
  }

  /**
   * Gets the canonical stringified name of the class the provided ResourceController belongs to.
   * @param controller the ResourceController instance whose class is to be named
   * @return the name string
   */
  public static String getNameFor(ResourceController controller) {
    return getNameFor(controller.getClass());
  }

  /**
   * Gets the default generated stringified name of the class the provided ResourceController
   * belongs to.
   * @param controller the ResourceController instance whose class is to be named
   * @return the name string
   */
  public static String getDefaultNameFor(ResourceController controller) {
    return getDefaultNameFor(controller.getClass());
  }

  /**
   * Gets the default generated stringified name of the provided ResourceController descendant
   * class.
   * @param controllerClass the class to be named
   * @return the name string
   */
  public static String getDefaultNameFor(Class<? extends ResourceController> controllerClass) {
    return getDefaultResourceControllerName(controllerClass.getSimpleName());
  }

  /**
   * Generates the default name of the named resource controller.
   * @param rcControllerClassName the name of the resource controller
   * @return the generated name
   */
  public static String getDefaultResourceControllerName(String rcControllerClassName) {
    // if the name is fully qualified, extract the simple class name
    final var lastDot = rcControllerClassName.lastIndexOf('.');
    if (lastDot > 0) {
      rcControllerClassName = rcControllerClassName.substring(lastDot + 1);
    }
    return rcControllerClassName.toLowerCase(Locale.ROOT);
  }
}
