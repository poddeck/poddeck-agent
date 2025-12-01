package io.poddeck.agent.deployment;

import com.google.api.client.util.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.poddeck.common.*;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class DeploymentFactory {
  private final CoreV1Api coreApi;
  private final Log log;

  public Deployment assembleDeployment(V1Deployment deployment) {
    return Deployment.newBuilder()
      .setMetadata(assembleMetadata(deployment))
      .setSpec(assembleSpec(deployment))
      .setStatus(assembleStatus(deployment))
      .addAllEvents(assembleDeploymentEvents(deployment))
      .build();
  }

  private DeploymentMetadata assembleMetadata(V1Deployment deployment) {
    if (deployment.getMetadata() == null) {
      return DeploymentMetadata.newBuilder().build();
    }
    var metadata = deployment.getMetadata();
    return DeploymentMetadata.newBuilder()
      .setName(Optional.ofNullable(metadata.getName()).orElse(""))
      .setNamespace(Optional.ofNullable(metadata.getNamespace()).orElse(""))
      .putAllLabels(metadata.getLabels() != null ?
        metadata.getLabels() : Collections.emptyMap())
      .putAllAnnotations(metadata.getAnnotations() != null ?
        metadata.getAnnotations() : Collections.emptyMap())
      .build();
  }

  private DeploymentSpec assembleSpec(V1Deployment deployment) {
    if (deployment.getSpec() == null) {
      return DeploymentSpec.newBuilder().build();
    }
    var spec = deployment.getSpec();

    return DeploymentSpec.newBuilder()
      .setReplicas(spec.getReplicas() != null ? spec.getReplicas() : 0)
      .setSelector(assembleSelector(spec.getSelector()))
      .setTemplate(assemblePodTemplate(spec.getTemplate()))
      .build();
  }

  private DeploymentSelector assembleSelector(V1LabelSelector selector) {
    if (selector == null) {
      return DeploymentSelector.newBuilder().build();
    }
    return DeploymentSelector.newBuilder()
      .putAllMatchLabels(selector.getMatchLabels() != null ?
        selector.getMatchLabels() : Collections.emptyMap())
      .build();
  }

  private PodTemplate assemblePodTemplate(V1PodTemplateSpec template) {
    if (template == null) {
      return PodTemplate.newBuilder().build();
    }
    var metadata = Optional.ofNullable(template.getMetadata())
      .orElse(new V1ObjectMeta());
    var spec = Optional.ofNullable(template.getSpec()).orElse(new V1PodSpec());
    return PodTemplate.newBuilder()
      .setMetadata(PodMetadata.newBuilder()
        .setName(Optional.ofNullable(metadata.getName()).orElse(""))
        .setNamespace(Optional.ofNullable(metadata.getNamespace()).orElse(""))
        .putAllLabels(metadata.getLabels() != null ?
          metadata.getLabels() : Collections.emptyMap())
        .putAllAnnotations(metadata.getAnnotations() != null ?
          metadata.getAnnotations() : Collections.emptyMap())
        .build())
      .setSpec(assemblePodSpec(spec))
      .build();
  }

  private PodSpec assemblePodSpec(V1PodSpec spec) {
    if (spec == null) {
      return PodSpec.newBuilder().build();
    }
    var containers = Optional.ofNullable(spec.getContainers())
      .orElse(Lists.newArrayList()).stream().map(this::assembleContainer).toList();
    return PodSpec.newBuilder()
      .setNodeName(Optional.ofNullable(spec.getNodeName()).orElse(""))
      .addAllContainers(containers)
      .build();
  }

  private Container assembleContainer(V1Container container) {
    return Container.newBuilder()
      .setName(container.getName())
      .setImage(Optional.ofNullable(container.getImage()).orElse(""))
      .addAllCommand(Optional.ofNullable(container.getCommand())
        .orElse(Lists.newArrayList()))
      .addAllArgs(Optional.ofNullable(container.getArgs())
        .orElse(Lists.newArrayList()))
      .putAllEnv(container.getEnv() != null ?
        container.getEnv().stream().collect(Collectors.toMap(
          V1EnvVar::getName, v -> Optional.ofNullable(v.getValue()).orElse("")
        )) :
        Collections.emptyMap()
      )
      .build();
  }

  private DeploymentStatus assembleStatus(V1Deployment deployment) {
    if (deployment.getStatus() == null) {
      return DeploymentStatus.newBuilder().build();
    }
    var status = deployment.getStatus();
    var conditions = status.getConditions() != null ?
      status.getConditions().stream()
        .map(this::assembleDeploymentCondition).toList() :
      Lists.<DeploymentCondition>newArrayList();
    var age = 0L;
    if (deployment.getMetadata() != null &&
      deployment.getMetadata().getCreationTimestamp() != null
    ) {
      age = System.currentTimeMillis() -
        deployment.getMetadata().getCreationTimestamp().toEpochSecond() * 1000L;
    }
    return DeploymentStatus.newBuilder()
      .setReplicas(Optional.ofNullable(status.getReplicas()).orElse(0))
      .setUpdatedReplicas(Optional.ofNullable(status.getUpdatedReplicas())
        .orElse(0))
      .setReadyReplicas(Optional.ofNullable(status.getReadyReplicas()).orElse(0))
      .setAvailableReplicas(Optional.ofNullable(status.getAvailableReplicas())
        .orElse(0))
      .setUnavailableReplicas(Optional.ofNullable(status.getUnavailableReplicas())
        .orElse(0))
      .setUpdatedRevision(Optional.ofNullable(status.getUpdatedReplicas())
        .map(Object::toString).orElse(""))
      .setCurrentRevision(Optional.ofNullable(status.getObservedGeneration())
        .map(Object::toString).orElse(""))
      .setAge(age)
      .addAllConditions(conditions)
      .build();
  }

  private DeploymentCondition assembleDeploymentCondition(
    V1DeploymentCondition condition
  ) {
    if (condition == null) {
      return DeploymentCondition.newBuilder().build();
    }
    return DeploymentCondition.newBuilder()
      .setType(Optional.ofNullable(condition.getType()).orElse(""))
      .setStatus(Optional.ofNullable(condition.getStatus()).orElse(""))
      .setReason(Optional.ofNullable(condition.getReason()).orElse(""))
      .setMessage(Optional.ofNullable(condition.getMessage()).orElse(""))
      .build();
  }

  private List<DeploymentEvent> assembleDeploymentEvents(V1Deployment deployment) {
    try {
      var metadata = deployment.getMetadata();
      if (metadata == null) {
        return Lists.newArrayList();
      }
      var namespace = metadata.getNamespace();
      var name = metadata.getName();
      var fieldSelector = "involvedObject.kind=Deployment,involvedObject.name=" + name;
      var items = coreApi.listNamespacedEvent(namespace)
        .fieldSelector(fieldSelector)
        .execute().getItems();
      return items.stream()
        .map(this::assembleDeploymentEvent)
        .toList();
    } catch (Exception exception) {
      log.processError(exception);
      return Lists.newArrayList();
    }
  }

  private DeploymentEvent assembleDeploymentEvent(CoreV1Event event) {
    long timestamp = 0;
    if (event.getLastTimestamp() != null) {
      timestamp = event.getLastTimestamp().toInstant().toEpochMilli();
    } else if (event.getEventTime() != null) {
      timestamp = event.getEventTime().toInstant().toEpochMilli();
    }
    return DeploymentEvent.newBuilder()
      .setType(Optional.ofNullable(event.getType()).orElse(""))
      .setReason(Optional.ofNullable(event.getReason()).orElse(""))
      .setMessage(Optional.ofNullable(event.getMessage()).orElse(""))
      .setTimestamp(timestamp)
      .setSource(event.getSource() != null &&
        event.getSource().getComponent() != null
        ? event.getSource().getComponent() : "")
      .build();
  }
}
