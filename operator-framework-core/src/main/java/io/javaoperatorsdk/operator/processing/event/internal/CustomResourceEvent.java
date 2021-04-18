package io.javaoperatorsdk.operator.processing.event.internal;

import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.Watcher;
import io.javaoperatorsdk.operator.processing.cache.CustomResourceID;
import io.javaoperatorsdk.operator.processing.event.AbstractEvent;

public class CustomResourceEvent extends AbstractEvent {

  private final Watcher.Action action;
  private final CustomResource customResource;

  public CustomResourceEvent(
      Watcher.Action action,
      CustomResource resource,
      CustomResourceEventSource customResourceEventSource) {
    super(CustomResourceID.fromCustomResource(resource), customResourceEventSource);
    this.action = action;
    this.customResource = resource;
  }

  public Watcher.Action getAction() {
    return action;
  }

  public String resourceUid() {
    return getCustomResource().getMetadata().getUid();
  }

  @Override
  public String toString() {
    return "CustomResourceEvent{"
        + "action="
        + action
        + ", resource=[ name="
        + getCustomResource().getMetadata().getName()
        + ", kind="
        + getCustomResource().getKind()
        + ", apiVersion="
        + getCustomResource().getApiVersion()
        + " ,resourceVersion="
        + getCustomResource().getMetadata().getResourceVersion()
        + ", markedForDeletion: "
        + (getCustomResource().getMetadata().getDeletionTimestamp() != null
            && !getCustomResource().getMetadata().getDeletionTimestamp().isEmpty())
        + " ]}";
  }

  public CustomResource getCustomResource() {
    return customResource;
  }
}
