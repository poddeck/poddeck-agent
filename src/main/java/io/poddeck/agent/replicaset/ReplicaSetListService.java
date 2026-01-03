package io.poddeck.agent.replicaset;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.ReplicaSetListRequest;
import io.poddeck.common.ReplicaSetListResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class ReplicaSetListService implements Service<ReplicaSetListRequest> {
  private final AppsV1Api appsApi;
  private final ReplicaSetFactory replicaSetFactory;

  @Override
  public void process(
    CommunicationClient client, String requestId, ReplicaSetListRequest request
  ) throws Exception {
    var replicaSetList = appsApi.listReplicaSetForAllNamespaces().execute().getItems();
    var replicaSets = replicaSetList.stream()
      .map(replicaSetFactory::assembleReplicaSet).toList();
    client.send(requestId, ReplicaSetListResponse.newBuilder()
      .addAllItems(replicaSets)
      .build());
  }
}
