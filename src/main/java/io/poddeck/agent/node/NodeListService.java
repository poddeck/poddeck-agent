package io.poddeck.agent.node;

import com.google.api.client.util.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeCondition;
import io.poddeck.agent.capacity.Capacity;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.*;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class NodeListService implements Service<NodeListRequest> {
  private final CoreV1Api coreApi;
  private final Log log;

  @Override
  public void process(
    CommunicationClient client, String requestId,
    NodeListRequest nodeListRequest
  ) throws Exception {
    var nodes = coreApi.listNode().execute().getItems()
      .stream().map(this::assembleNode).toList();
    client.send(requestId, NodeListResponse.newBuilder()
      .addAllItems(nodes).build());
  }

  private Node assembleNode(V1Node node) {
    return Node.newBuilder()
      .setMetadata(assembleNodeMetadata(node))
      .setStatus(assembleNodeStatus(node))
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
      .setCondition(assembleNodeCondition(node))
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

  private NodeCondition assembleNodeCondition(V1Node node) {
    if (node.getStatus().getConditions() == null) {
      return NodeCondition.newBuilder().build();
    }
    var conditions = node.getStatus().getConditions();
    return NodeCondition.newBuilder()
      .setIsReady(conditions.stream()
        .anyMatch(condition -> "Ready".equals(condition.getType()) &&
          "True".equals(condition.getStatus())))
      .addAllConditions(conditions.stream()
        .filter(condition -> "True".equals(condition.getStatus()))
        .map(V1NodeCondition::getType).toList())
      .build();
  }
}