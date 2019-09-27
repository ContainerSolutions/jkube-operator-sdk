package com.github.containersolutions.operator.sample;

import com.github.containersolutions.operator.Operator;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;

public class PureJavaApplicationRunner {

    public static void main(String[] args) {
        Operator operator = new Operator(new DefaultKubernetesClient());
        operator.registerController(new CustomServiceController());
    }
}
