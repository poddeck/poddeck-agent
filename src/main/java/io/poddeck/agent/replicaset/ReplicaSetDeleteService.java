package io.poddeck.agent.replicaset;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.ReplicaSetDeleteRequest;
import io.poddeck.common.ReplicaSetDeleteResponse;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class ReplicaSetDeleteService implements Service<ReplicaSetDeleteRequest> {
  private final AppsV1Api appsApi;
  private final Log log;

  @Override
  public void process(
    CommunicationClient client, String requestId,
    ReplicaSetDeleteRequest request
  ) throws Exception {
    try {
      appsApi.deleteNamespacedReplicaSet(request.getReplicaSet(),
        request.getNamespace()).execute();
      client.send(requestId, ReplicaSetDeleteResponse.newBuilder()
        .setSuccess(true).build());
    } catch (Exception exception) {
      log.processError(exception);
      client.send(requestId, ReplicaSetDeleteResponse.newBuilder()
        .setSuccess(false).build());
    }
  }
}
