package io.javaoperatorsdk.quarkus.extension;

import io.javaoperatorsdk.operator.api.config.ControllerConfiguration;
import io.quarkus.runtime.annotations.Recorder;
import java.util.List;
import java.util.function.Supplier;

@Recorder
public class ConfigurationServiceRecorder {

  public Supplier<QuarkusConfigurationService> configurationServiceSupplier(
      List<ControllerConfiguration> controllerConfigs) {
    return () -> new QuarkusConfigurationService(controllerConfigs);
  }
}
