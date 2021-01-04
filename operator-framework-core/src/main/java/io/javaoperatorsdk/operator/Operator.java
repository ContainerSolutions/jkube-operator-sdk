package io.javaoperatorsdk.operator;

import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.CustomResourceDoneable;
import io.fabric8.kubernetes.client.CustomResourceList;
import io.fabric8.kubernetes.client.dsl.internal.CustomResourceOperationsImpl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.processing.retry.Retry;
import java.util.Map;

public interface Operator {

  <R extends CustomResource> void register(ResourceController<R> controller)
      throws OperatorException;

  <R extends CustomResource> void registerControllerForAllNamespaces(
      ResourceController<R> controller, Retry retry) throws OperatorException;

  <R extends CustomResource> void registerControllerForAllNamespaces(
      ResourceController<R> controller) throws OperatorException;

  <R extends CustomResource> void registerController(
      ResourceController<R> controller, Retry retry, String... targetNamespaces)
      throws OperatorException;

  <R extends CustomResource> void registerController(
      ResourceController<R> controller, String... targetNamespaces) throws OperatorException;

  Map<Class<? extends CustomResource>, CustomResourceOperationsImpl> getCustomResourceClients();

  <T extends CustomResource, L extends CustomResourceList<T>, D extends CustomResourceDoneable<T>>
      CustomResourceOperationsImpl<T, L, D> getCustomResourceClients(Class<T> customResourceClass);
}
