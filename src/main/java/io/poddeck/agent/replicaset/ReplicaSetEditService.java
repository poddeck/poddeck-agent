package io.poddeck.agent.replicaset;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1ReplicaSet;
import io.kubernetes.client.util.Yaml;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.ReplicaSetEditRequest;
import io.poddeck.common.ReplicaSetEditResponse;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class ReplicaSetEditService implements Service<ReplicaSetEditRequest> {
  private final AppsV1Api appsApi;
  private final Log log;

  @Override
  public void process(
    CommunicationClient client, String requestId,
    ReplicaSetEditRequest request
  ) throws Exception {
    try {
      var replicaSet = Yaml.loadAs(request.getRaw(), V1ReplicaSet.class);
      appsApi.replaceNamespacedReplicaSet(request.getReplicaSet(),
        request.getNamespace(), replicaSet).execute();
      client.send(requestId, ReplicaSetEditResponse.newBuilder()
        .setSuccess(true).build());
    } catch (Exception exception) {
      log.processError(exception);
      client.send(requestId, ReplicaSetEditResponse.newBuilder()
        .setSuccess(false).build());
    }
  }
}
