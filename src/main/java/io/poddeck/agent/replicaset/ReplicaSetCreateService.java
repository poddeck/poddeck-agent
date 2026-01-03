package io.poddeck.agent.replicaset;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1ReplicaSet;
import io.kubernetes.client.util.Yaml;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.ReplicaSetCreateRequest;
import io.poddeck.common.ReplicaSetCreateResponse;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class ReplicaSetCreateService implements Service<ReplicaSetCreateRequest> {
  private final AppsV1Api appsApi;
  private final Log log;

  @Override
  public void process(
    CommunicationClient client, String requestId,
    ReplicaSetCreateRequest request
  ) throws Exception {
    try {
      var replicaSet = Yaml.loadAs(request.getRaw(), V1ReplicaSet.class);
      var metadata = replicaSet.getMetadata();
      var namespace = metadata != null && metadata.getNamespace() != null ?
        metadata.getNamespace() : "default";
      appsApi.createNamespacedReplicaSet(namespace, replicaSet).execute();
      client.send(requestId, ReplicaSetCreateResponse.newBuilder()
        .setSuccess(true).setNamespace(namespace)
        .setReplicaSet(metadata != null && metadata.getName() != null ?
          metadata.getName() : "")
        .build());
    } catch (Exception exception) {
      log.processError(exception);
      client.send(requestId, ReplicaSetCreateResponse.newBuilder()
        .setSuccess(false).build());
    }
  }
}
