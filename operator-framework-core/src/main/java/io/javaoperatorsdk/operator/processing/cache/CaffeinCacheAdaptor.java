package io.javaoperatorsdk.operator.processing.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.fabric8.kubernetes.client.CustomResource;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class CaffeinCacheAdaptor implements ResourceCache {

  private final Cache<String, CustomResource> cache;

  public CaffeinCacheAdaptor(Cache<String, CustomResource> cache) {
    this.cache = cache;
  }

  public CaffeinCacheAdaptor() {
    this(Caffeine.newBuilder().expireAfterWrite(15, TimeUnit.MINUTES).build());
  }

  public void cacheResource(CustomResource resource) {
    cache.put(resource.getMetadata().getUid(), resource);
  }

  public Optional<CustomResource> getLatestResource(String uuid) {
    return Optional.ofNullable(cache.getIfPresent(uuid));
  }

  public void evict(String uuid) {
    cache.invalidate(uuid);
  }
}
