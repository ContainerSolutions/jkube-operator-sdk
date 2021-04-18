package io.javaoperatorsdk.operator.processing.cache;

import io.fabric8.kubernetes.client.CustomResource;

import java.util.Objects;

/** Since we need to */
public class CustomResourceID {

  private final String namespace;
  private final String name;

  public static CustomResourceID fromCustomResource(CustomResource customResource) {
    var metadata = customResource.getMetadata();
    return new CustomResourceID(metadata.getNamespace(),metadata.getName());
  }

  CustomResourceID(String namespace, String name) {
    this.namespace = namespace;
    this.name = name;
  }

  public String getNamespace() {
    return namespace;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return "CustomResourceID{" +
            "namespace='" + namespace + '\'' +
            ", name='" + name + '\'' +
            '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CustomResourceID that = (CustomResourceID) o;
    return Objects.equals(namespace, that.namespace) && Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(namespace, name);
  }
}
