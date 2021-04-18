package io.javaoperatorsdk.operator.processing.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.javaoperatorsdk.operator.processing.KubernetesResourceUtils;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PassThroughResourceCache<R extends CustomResource> {

  private static final Logger log = LoggerFactory.getLogger(PassThroughResourceCache.class);

  private ResourceCache resourceCache;
  private MixedOperation<R, KubernetesResourceList<R>, Resource<R>> client;
  private final ObjectMapper objectMapper;

  public PassThroughResourceCache(
      ResourceCache resourceCache, MixedOperation client, ObjectMapper objectMapper) {
    this.resourceCache = resourceCache;
    this.client = client;
    this.objectMapper = objectMapper;
  }

  public void cacheResource(CustomResource resource) {
    resourceCache.cacheResource(resource);
    // todo discuss  this + alternatives
  }

  public void cacheResource(CustomResource resource, Predicate<CustomResource> predicate) {
    // todo get + lock
    if (predicate.test(
        resourceCache.getLatestResource(KubernetesResourceUtils.getUID(resource)).get())) {
      log.trace("Update cache after condition is true: {}", resource);
      resourceCache.cacheResource(resource);
    }
  }

  public Optional<CustomResource> getLatestResource(CustomResourceID id) {
//     todo
    Optional<CustomResource> resource = resourceCache.getLatestResource();
    if (resource.isPresent()) {
      return Optional.of(clone(resource.get()));
    } else {
      // todo read from server
      return null;
    }
  }

  public void evict(String uuid) {
    resourceCache.evict(uuid);
  }

  private CustomResource clone(CustomResource customResource) {
    try {
      return objectMapper.readValue(
          objectMapper.writeValueAsString(customResource), customResource.getClass());
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }
}
