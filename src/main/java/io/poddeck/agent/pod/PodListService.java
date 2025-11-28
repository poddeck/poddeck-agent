package io.poddeck.agent.pod;

import com.google.api.client.util.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
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
public final class PodListService implements Service<PodListRequest> {
  private final Log log;
  private final CoreV1Api coreApi;

  @Override
  public void process(
    CommunicationClient client, String requestId,
    PodListRequest podListRequest
  ) throws Exception {
    var podList = coreApi.listPodForAllNamespaces().execute().getItems();
    var pods = podList.stream().map(this::assemblePod).toList();
    client.send(requestId, PodListResponse.newBuilder()
      .addAllItems(pods)
      .build());
  }

  private Pod assemblePod(V1Pod pod) {
    return Pod.newBuilder()
      .setMetadata(assemblePodMetadata(pod))
      .setSpec(assemblePodSpec(pod))
      .setStatus(assemblePodStatus(pod))
      .addAllEvents(assemblePodEvents(pod))
      .build();
  }

  private PodMetadata assemblePodMetadata(V1Pod pod) {
    if (pod.getMetadata() == null) {
      return PodMetadata.newBuilder().build();
    }
    var metadata = pod.getMetadata();
    return PodMetadata.newBuilder()
      .setName(metadata.getName())
      .setNamespace(metadata.getNamespace())
      .putAllLabels(metadata.getLabels() != null ?
        metadata.getLabels() : Collections.emptyMap())
      .putAllAnnotations(metadata.getAnnotations() != null ?
        metadata.getAnnotations() : Collections.emptyMap())
      .build();
  }

  private PodSpec assemblePodSpec(V1Pod pod) {
    if (pod.getSpec() == null) {
      return PodSpec.newBuilder().build();
    }
    var spec = pod.getSpec();
    var containers = spec.getContainers().stream()
      .map(this::assembleContainer).toList();
    return PodSpec.newBuilder()
      .setNodeName(spec.getNodeName() != null ? spec.getNodeName() : "")
      .addAllContainers(containers)
      .build();
  }

  private Container assembleContainer(V1Container container) {
    return Container.newBuilder()
      .setName(container.getName())
      .setImage(container.getImage() != null ? container.getImage() : "")
      .addAllCommand(container.getCommand() != null ?
        container.getCommand() : Lists.newArrayList())
      .addAllArgs(container.getArgs() != null ?
        container.getArgs() : Lists.newArrayList())
      .putAllEnv(container.getEnv() != null ?
        container.getEnv().stream().collect(Collectors.toMap(
          V1EnvVar::getName, v -> v.getValue() != null ? v.getValue() : "")) :
        Collections.emptyMap())
      .build();
  }

  private PodStatus assemblePodStatus(V1Pod pod) {
    if (pod.getStatus() == null) {
      return PodStatus.newBuilder().build();
    }
    var status = pod.getStatus();
    var conditions = status.getConditions() != null ?
      status.getConditions().stream().map(this::assemblePodCondition).toList() :
      Lists.<PodCondition>newArrayList();
    var statuses = status.getContainerStatuses() != null ?
      status.getContainerStatuses().stream().map(this::assemblePodContainerStatus).toList() :
      Lists.<PodContainerStatus>newArrayList();
    var age = 0L;
    if (pod.getMetadata() != null && pod.getMetadata().getCreationTimestamp() != null) {
      age = (System.currentTimeMillis() -
        pod.getMetadata().getCreationTimestamp().toEpochSecond() * 1000);
    }
    return PodStatus.newBuilder()
      .setPhase(status.getPhase() != null ? status.getPhase() : "")
      .addAllConditions(conditions)
      .addAllStatuses(statuses)
      .setHostIp(status.getHostIP() != null ? status.getHostIP() : "")
      .setPodIp(status.getPodIP() != null ? status.getPodIP() : "")
      .setAge(age)
      .setNode(pod.getSpec() != null && pod.getSpec().getNodeName() != null ?
        pod.getSpec().getNodeName() : "")
      .build();
  }

  private PodCondition assemblePodCondition(V1PodCondition condition) {
    return PodCondition.newBuilder()
      .setType(condition.getType())
      .setStatus(condition.getStatus())
      .setReason(condition.getReason() != null ? condition.getReason() : "")
      .setMessage(condition.getMessage() != null ? condition.getMessage() : "")
      .build();
  }

  private PodContainerStatus assemblePodContainerStatus(V1ContainerStatus status) {
    return PodContainerStatus.newBuilder()
      .setName(status.getName())
      .setImage(status.getImage())
      .setReady(status.getReady())
      .setState(status.getState() != null ?
        determineContainerState(status.getState()) : "")
      .setMessage(Optional.ofNullable(status.getState())
        .map(s -> s.getWaiting()).map(w -> w.getReason()).orElse(""))
      .setMessage(Optional.ofNullable(status.getState())
        .map(s -> s.getWaiting()).map(w -> w.getMessage()).orElse(""))
      .setRestartCount(status.getRestartCount())
      .build();
  }

  private String determineContainerState(V1ContainerState state) {
    if (state.getRunning() != null) {
      return "Running";
    } else if (state.getTerminated() != null) {
      return "Terminated";
    } else if (state.getWaiting() != null) {
      return "Waiting";
    }
    return "Unknown";
  }

  private List<PodEvent> assemblePodEvents(V1Pod pod) {
    try {
      var metadata = pod.getMetadata();
      if (metadata == null) {
        return Collections.emptyList();
      }
      var namespace = metadata.getNamespace();
      var name = metadata.getName();
      var fieldSelector = "involvedObject.kind=Pod,involvedObject.name=" + name;
      var eventList = coreApi.listNamespacedEvent(namespace)
        .fieldSelector(fieldSelector).execute().getItems();
      return eventList.stream()
        .map(this::assemblePodEvent).toList();
    } catch (Exception exception) {
      log.processError(exception);
      return Lists.newArrayList();
    }
  }

  private PodEvent assemblePodEvent(CoreV1Event event) {
    long timestamp = 0L;
    if (event.getLastTimestamp() != null) {
      timestamp = event.getLastTimestamp().toInstant().toEpochMilli();
    } else if (event.getEventTime() != null) {
      timestamp = event.getEventTime().toInstant().toEpochMilli();
    }
    return PodEvent.newBuilder()
      .setType(Optional.ofNullable(event.getType()).orElse(""))
      .setReason(Optional.ofNullable(event.getReason()).orElse(""))
      .setMessage(Optional.ofNullable(event.getMessage()).orElse(""))
      .setTimestamp(timestamp)
      .setSource(event.getSource() != null && event.getSource().getComponent() != null
        ? event.getSource().getComponent() : "")
      .build();
  }
}
