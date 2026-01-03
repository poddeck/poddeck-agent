package io.poddeck.agent.replicaset;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.ReplicaSetFindRequest;
import io.poddeck.common.ReplicaSetFindResponse;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class ReplicaSetFindService implements Service<ReplicaSetFindRequest> {
  private final AppsV1Api appsApi;
  private final ReplicaSetFactory replicaSetFactory;
  private final Log log;

  @Override
  public void process(
    CommunicationClient client, String requestId, ReplicaSetFindRequest request
  ) throws Exception {
    try {
      var replicaSet = appsApi.readNamespacedReplicaSet(request.getReplicaSet(),
        request.getNamespace()).execute();
      client.send(requestId, ReplicaSetFindResponse.newBuilder()
        .setSuccess(true)
        .setReplicaSet(replicaSetFactory.assembleReplicaSet(replicaSet))
        .build());
    } catch (Exception exception) {
      log.processError(exception);
      client.send(requestId, ReplicaSetFindResponse.newBuilder()
        .setSuccess(false).build());
    }
  }
}
