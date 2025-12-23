package io.poddeck.agent.node;

import com.google.api.client.util.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Node;
import io.poddeck.agent.capacity.Capacity;
import io.poddeck.common.*;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class NodeFactory {
  private final CoreV1Api coreApi;
  private final Log log;

  public Node assembleNode(V1Node node) {
    return Node.newBuilder()
      .setMetadata(assembleNodeMetadata(node))
      .setStatus(assembleNodeStatus(node))
      .addAllEvents(assembleNodeEvents(node))
      .build();
  }

  private NodeMetadata assembleNodeMetadata(V1Node node) {
    if (node.getMetadata() == null) {
      return NodeMetadata.newBuilder().build();
    }
    var metadata = node.getMetadata();
    return NodeMetadata.newBuilder()
      .setName(metadata.getName())
      .putAllLabels(metadata.getLabels() != null ?
        metadata.getLabels() : Collections.emptyMap())
      .putAllAnnotations(metadata.getAnnotations() != null ?
        metadata.getAnnotations() : Collections.emptyMap())
      .build();
  }

  private NodeStatus assembleNodeStatus(V1Node node) {
    if (node.getStatus() == null) {
      return NodeStatus.newBuilder().build();
    }
    return NodeStatus.newBuilder()
      .addAllAddresses(assembleNodeAddresses(node))
      .setCapacity(assembleNodeCapacity(node))
      .setInfo(assembleNodeInfo(node))
      .addAllConditions(assembleNodeConditions(node))
      .setAge(calculateAge(node))
      .build();
  }

  private List<NodeAddress> assembleNodeAddresses(V1Node node) {
    if (node.getStatus().getAddresses() == null) {
      return Lists.newArrayList();
    }
    return node.getStatus().getAddresses().stream()
      .map(address -> NodeAddress.newBuilder()
        .setType(address.getType())
        .setAddress(address.getAddress())
        .build())
      .toList();
  }

  private NodeCapacity assembleNodeCapacity(V1Node node) {
    try {
      if (node.getStatus().getCapacity() == null) {
        return NodeCapacity.newBuilder().build();
      }
      var capacity = Capacity.of(node.getStatus().getCapacity());
      var pods = coreApi.listPodForAllNamespaces()
        .fieldSelector("spec.nodeName=" + node.getMetadata().getName())
        .execute().getItems();
      var cpuAllocated = 0L;
      var memoryAllocated = 0L;
      for (var pod : pods) {
        if (pod.getSpec() == null) {
          continue;
        }
        for (var container : pod.getSpec().getContainers()) {
          var resources = container.getResources();
          if (resources != null && resources.getRequests() != null) {
            var requests = Capacity.of(resources.getRequests());
            cpuAllocated += requests.cpu();
            memoryAllocated += requests.memory();
          }
        }
      }
      return NodeCapacity.newBuilder()
        .setTotalCpu(capacity.cpu())
        .setTotalMemory(capacity.memory())
        .setAllocatedCpu(cpuAllocated)
        .setAllocatedMemory(memoryAllocated)
        .build();
    } catch (Exception exception) {
      log.processError(exception);
      return null;
    }
  }

  private NodeInfo assembleNodeInfo(V1Node node) {
    if (node.getStatus().getNodeInfo() == null) {
      return NodeInfo.newBuilder().build();
    }
    var nodeInfo = node.getStatus().getNodeInfo();
    return NodeInfo.newBuilder()
      .setArchitecture(nodeInfo.getArchitecture())
      .setOsImage(nodeInfo.getOsImage())
      .setContainerRuntimeVersion(nodeInfo.getContainerRuntimeVersion())
      .setKubeletVersion(nodeInfo.getKubeletVersion())
      .setOperatingSystem(nodeInfo.getOperatingSystem())
      .build();
  }

  private List<NodeCondition> assembleNodeConditions(V1Node node) {
    if (node.getStatus().getConditions() == null) {
      return Lists.newArrayList();
    }
    return node.getStatus().getConditions().stream()
      .map(condition -> NodeCondition.newBuilder()
        .setType(condition.getType())
        .setStatus(condition.getStatus())
        .setReason(condition.getReason() != null ? condition.getReason() : "")
        .setMessage(condition.getMessage() != null ? condition.getMessage() : "")
        .setLastHeartbeat(toEpochSeconds(condition.getLastHeartbeatTime()))
        .setLastTransition(toEpochSeconds(condition.getLastTransitionTime()))
        .build())
      .toList();
  }

  private List<NodeEvent> assembleNodeEvents(V1Node node) {
    try {
      if (node.getMetadata() == null || node.getMetadata().getName() == null) {
        return Lists.newArrayList();
      }
      var events = coreApi.listEventForAllNamespaces()
        .fieldSelector("involvedObject.name=" + node.getMetadata().getName() +
          ",involvedObject.kind=Node")
        .execute().getItems();
      return events.stream()
        .map(event -> NodeEvent.newBuilder()
          .setType(event.getType() != null ? event.getType() : "")
          .setReason(event.getReason() != null ? event.getReason() : "")
          .setMessage(event.getMessage() != null ? event.getMessage() : "")
          .setTimestamp(toEpochSeconds(event.getLastTimestamp()))
          .setSource(event.getSource() != null && event.getSource().getComponent() != null ?
            event.getSource().getComponent() : "")
          .build())
        .toList();
    } catch (Exception exception) {
      log.processError(exception);
      return Lists.newArrayList();
    }
  }

  private long calculateAge(V1Node node) {
    if (node.getMetadata() == null || node.getMetadata().getCreationTimestamp() == null) {
      return 0L;
    }
    return toEpochSeconds(node.getMetadata().getCreationTimestamp());
  }

  private long toEpochSeconds(OffsetDateTime dateTime) {
    if (dateTime == null) {
      return 0L;
    }
    return dateTime.toEpochSecond();
  }
}