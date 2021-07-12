package io.javaoperatorsdk.operator.api;

import io.fabric8.kubernetes.client.CustomResource;

/**
 * Encapsulates information about the logic executed when instances of the given custom resource are
 * deleted. Returned from {@link ResourceController#createOrUpdateResource}. Informs the framework
 * whether the finalizer associated with the custom resource can be deleted to allow K8s to delete
 * the resource {@link DeleteControl#DEFAULT_DELETE}, or if the finalizer should stay in place
 * {@link DeleteControl#NO_FINALIZER_REMOVAL}.
 */
public enum DeleteControl {
  DEFAULT_DELETE,
  NO_FINALIZER_REMOVAL
}
