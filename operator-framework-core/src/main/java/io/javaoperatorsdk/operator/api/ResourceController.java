package io.javaoperatorsdk.operator.api;

import io.fabric8.kubernetes.client.CustomResource;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;

/**
 * Interface to be implemented by user-created resource controller classes that operate user-defined
 * custom resource objects.
 *
 * @param <R> the {@link CustomResource} that drives the controller
 */
public interface ResourceController<R extends CustomResource> {

  /**
   * This method is called when a managed custom resource instance has been marked for deletion.
   * After execution the custom resource finalizer is automatically removed unless the return value
   * is {@link DeleteControl#NO_FINALIZER_REMOVAL}.
   *
   * @param resource the {@link CustomResource} instance that is marked for deletion
   * @param context the context in which the operation is executed
   * @return either {@link DeleteControl#DEFAULT_DELETE} that indicates that the K8s finalizer on
   * the resource should automatically be removed as well, or
   * {@link DeleteControl#NO_FINALIZER_REMOVAL} if you don't want to remove the K8s finalizer, which
   * will prevent resource deletion by K8s
   */
  default DeleteControl deleteResource(R resource, Context<R> context) {
    return DeleteControl.DEFAULT_DELETE;
  }

  /**
   * This method is called each time a managed K8s object has been created or updated. The
   * implementation of this operation must be idempotent, as multiple calls can occur based on
   * current cluster status.
   *
   * @param resource the the {@link CustomResource} instance that has been created or updated
   * @param context the context with which the operation is executed
   * @return either an {@link UpdateControl} object, or {@link UpdateControl#noUpdate()} if no
   * updates need to be sent to the API server.
   */
  UpdateControl<R> createOrUpdateResource(R resource, Context<R> context);

  /**
   * Initialization function that registers event sources using a manager object.
   *
   * @param eventSourceManager the {@link EventSourceManager} which handles this controller and with
   * which event sources can be registered
   */
  default void init(EventSourceManager eventSourceManager) {}
}
