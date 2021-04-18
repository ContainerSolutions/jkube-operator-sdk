package io.javaoperatorsdk.operator.processing.cache;

import io.fabric8.kubernetes.client.CustomResource;
import java.util.Optional;

public interface ResourceCache {

  void cacheResource(CustomResource resource);

  Optional<CustomResource> getLatestResource(String uuid);

  void evict(String uuid);
}
