package io.javaoperatorsdk.operator.api;

import io.fabric8.kubernetes.client.CustomResource;

/**
 * Encapsulates information about the logic executed when instances of the given custom resource are
 * created or updated. Returned from {@link ResourceController#createOrUpdateResource}.
 *
 * @param <T> the class of the {@link CustomResource} that is created or updated
 */
public class UpdateControl<T extends CustomResource> {

  /**
   * The {@link CustomResource} that is being created or updated
   */
  private final T customResource;

  /**
   * Indicates whether the status sub-resource of the custom resource has been updated
   */
  private final boolean updateStatusSubResource;

  /**
   * Indicates whether the custom resource itself has been updated
   */
  private final boolean updateCustomResource;

  /**
   * Instantiates an object representing the logic that has been executed as the provided custom
   * resource is created or updated, and whether that logic has updated the resource itself, or its
   * status sub-resource.
   *
   * @param customResource the {@link CustomResource} instance that has been created or updated
   * @param updateStatusSubResource whether the status sub-resource has been updated
   * @param updateCustomResource whether the custom resource itself has been updated
   */
  private UpdateControl(
      T customResource, boolean updateStatusSubResource, boolean updateCustomResource
  ) {
    if ((updateCustomResource || updateStatusSubResource) && customResource == null) {
      throw new IllegalArgumentException("CustomResource cannot be null in case of update");
    }
    this.customResource = customResource;
    this.updateStatusSubResource = updateStatusSubResource;
    this.updateCustomResource = updateCustomResource;
  }

  /**
   * Returns an object representing that the given custom resource has been updated, but not its
   * status sub-resource.
   *
   * @param customResource the {@link CustomResource} descendant instance to be updated
   * @param <T> the {@link CustomResource} descendant user-provided class of the resource
   * @return an UpdateControl object representing the update
   */
  public static <T extends CustomResource> UpdateControl<T> updateCustomResource(T customResource) {
    return new UpdateControl<>(customResource, false, true);
  }

  /**
   * Returns an object representing that the status sub-resource of the given custom resource has
   * been updated, but not the resource itself.
   *
   * @param customResource the {@link CustomResource} descendant instance to be updated
   * @param <T> the {@link CustomResource} descendant user-provided class of the resource
   * @return an UpdateControl object representing the update
   */
  public static <T extends CustomResource> UpdateControl<T> updateStatusSubResource(
      T customResource) {
    return new UpdateControl<>(customResource, true, false);
  }

  /**
   * Returns an object representing that the given custom resource has been updated, along with its
   * status sub-resource.
   *
   * @param customResource the {@link CustomResource} descendant instance to be updated
   * @param <T> the {@link CustomResource} descendant user-provided class of the resource
   * @return an UpdateControl object representing the update
   */
  public static <T extends CustomResource> UpdateControl<T> updateCustomResourceAndStatus(
      T customResource) {
    return new UpdateControl<>(customResource, true, true);
  }
 /**
  * Returns an object representing that neither the given custom resource has been updated, nor its
  * status sub-resource.
  *
  * @param <T> the {@link CustomResource} descendant user-provided class of the resource
  * @return an UpdateControl object representing the update
  */
  public static <T extends CustomResource> UpdateControl<T> noUpdate() {
    return new UpdateControl<>(null, false, false);
  }

  /**
   * Gets the custom resource instance that has been created or updated.
   *
   * @return the created or updated resource
   */
  public T getCustomResource() {
    return customResource;
  }

  /**
   * Gets a boolean representing whether the status sub-resource of the custom resource has been
   * updated.
   *
   * @return whether the status sub-resource has been updated
   */
  public boolean isUpdateStatusSubResource() {
    return updateStatusSubResource;
  }

  /**
   * Gets a boolean representing whether the custom resource itself has been updated.
   *
   * @return whether the custom resource has been updated
   */
  public boolean isUpdateCustomResource() {
    return updateCustomResource;
  }

  /**
   * Gets a boolean representing whether the both the custom resource and its status sub-resource
   * have been updated.
   *
   * @return whether both the custom resource and its status sub-resource have been updated
   */
  public boolean isUpdateCustomResourceAndStatusSubResource() {
    return updateCustomResource && updateStatusSubResource;
  }
}
