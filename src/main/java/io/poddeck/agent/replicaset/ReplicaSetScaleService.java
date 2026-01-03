package io.poddeck.agent.replicaset;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Scale;
import io.kubernetes.client.openapi.models.V1ScaleSpec;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.ReplicaSetScaleRequest;
import io.poddeck.common.ReplicaSetScaleResponse;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class ReplicaSetScaleService implements Service<ReplicaSetScaleRequest> {
  private final AppsV1Api appsApi;
  private final Log log;

  @Override
  public void process(
    CommunicationClient client, String requestId,
    ReplicaSetScaleRequest request
  ) throws Exception {
    try {
      var scale = new V1Scale()
        .apiVersion("autoscaling/v1").kind("Scale")
        .metadata(new V1ObjectMeta()
          .name(request.getReplicaSet())
          .namespace(request.getNamespace()))
        .spec(new V1ScaleSpec().replicas(request.getReplicas()));
      appsApi.replaceNamespacedReplicaSetScale(request.getReplicaSet(),
        request.getNamespace(), scale).execute();
      client.send(requestId, ReplicaSetScaleResponse.newBuilder()
        .setSuccess(true).build());
    } catch (Exception exception) {
      log.processError(exception);
      client.send(requestId, ReplicaSetScaleResponse.newBuilder()
        .setSuccess(false).build());
    }
  }
}
