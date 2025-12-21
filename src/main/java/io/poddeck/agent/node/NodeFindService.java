package io.poddeck.agent.node;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.NodeFindRequest;
import io.poddeck.common.NodeFindResponse;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class NodeFindService implements Service<NodeFindRequest> {
  private final CoreV1Api coreApi;
  private final NodeFactory nodeFactory;
  private final Log log;

  @Override
  public void process(
    CommunicationClient client, String requestId, NodeFindRequest request
  ) throws Exception {
    try {
      var node = coreApi.readNode(request.getName()).execute();
      client.send(requestId, NodeFindResponse.newBuilder()
        .setSuccess(true).setNode(nodeFactory.assembleNode(node)).build());
    } catch (Exception exception) {
      log.processError(exception);
      client.send(requestId, NodeFindResponse.newBuilder()
        .setSuccess(false).build());
    }
  }
}