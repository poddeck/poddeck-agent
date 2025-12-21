package io.poddeck.agent.node;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.NodeListRequest;
import io.poddeck.common.NodeListResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class NodeListService implements Service<NodeListRequest> {
  private final CoreV1Api coreApi;
  private final NodeFactory nodeFactory;

  @Override
  public void process(
    CommunicationClient client, String requestId, NodeListRequest request
  ) throws Exception {
    var nodes = coreApi.listNode().execute().getItems()
      .stream().map(nodeFactory::assembleNode).toList();
    client.send(requestId, NodeListResponse.newBuilder()
      .addAllItems(nodes).build());
  }
}