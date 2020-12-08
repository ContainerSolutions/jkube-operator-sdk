package io.javaoperatorsdk.quarkus.extension;

import java.util.List;
import java.util.function.Supplier;

import io.javaoperatorsdk.operator.api.config.ConfigurationService;
import io.javaoperatorsdk.operator.api.config.ControllerConfiguration;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ConfigurationServiceRecorder {
    
    public Supplier<ConfigurationService> configurationServiceSupplier(List<ControllerConfiguration> controllerConfigs) {
        return () -> new QuarkusConfigurationService(controllerConfigs);
    }
}