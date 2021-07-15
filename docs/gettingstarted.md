## Getting Started

Ready to build your first operator or you've done it before with other frameworks/languages this guide will help you get a taste of creating and running an operator with the Java Operator SDK in under 30 minutes.

We recommend you start with building and deploying one of our sample applications to your Kubernetes cluster of choice. Let's see how that works!

#### Prerequisites
* Docker installed on your machine so you can build images. You can find the appropriate page for your OS here: [https://docs.docker.com/get-docker/](https://docs.docker.com/get-docker/).
* Any Kubernetes cluster. Feel free to use what you're familiar with. If you're new to Kubernetes you can install it locally using Docker for Mac or Windows or [minikube](https://kubernetes.io/docs/tasks/tools/install-minikube/)  on any OS.
* JDK version 11 or higher and Maven 3.6.x. Make sure `JAVA_HOME` and `MVN_HOME` are set correctly.
* Have [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/) installed and configured to connect to the cluster. You will need cluster-admin privileges to be able to deploy the CRDs (see below).

#### Building a sample
Our sample projects are located in the [samples repo](https://github.com/java-operator-sdk/samples).
Pick one of the samples that feels most relevant for you.

There are two ways to go about when running one of the samples. At some point you will want to use both, but it's up to you which one to start with:
1. Run the code locally from your IDE or Maven and have it connect to the Kubernetes API server. This is the way to go when developing an operator.
2. Build the docker image, push it to the registry (unless using local Kubernetes) and deploy to Kubernetes. This is the way to do a real deployment.

Let's start with #1 - Running the operator locally and connecting to a remove Kubernetes API
1. First the Kubernetes cluster will have to know about our custom resource(s). Each sample has a file called `k8s/crd.yaml`. This file contains the custom resource definitions (CRD) the operator needs. Run `kubectl apply -f k8s/crd.yaml` to deploy the CRDs. If you get an error saying you're not authorized to create CustomResourceDefinition objects then you need to get cluster-admin rights.
1. Once the CRDs are deployed you can go ahead and run the sample from your IDE. When running locally the operator will use `kubectl`'s configuration to connect to Kubernetes. You don't need to do anything to make this work. 