package io.poddeck.agent.deployment;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Scale;
import io.kubernetes.client.openapi.models.V1ScaleSpec;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.DeploymentScaleRequest;
import io.poddeck.common.DeploymentScaleResponse;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class DeploymentScaleService implements Service<DeploymentScaleRequest> {
  private final AppsV1Api appsApi;
  private final Log log;

  @Override
  public void process(
    CommunicationClient client, String requestId,
    DeploymentScaleRequest request
  ) throws Exception {
    try {
      var scale = new V1Scale()
        .apiVersion("autoscaling/v1").kind("Scale")
        .metadata(new V1ObjectMeta()
          .name(request.getDeployment())
          .namespace(request.getNamespace()))
        .spec(new V1ScaleSpec().replicas(request.getReplicas()));
      appsApi.replaceNamespacedDeploymentScale(request.getDeployment(),
        request.getNamespace(), scale).execute();
      client.send(requestId, DeploymentScaleResponse.newBuilder()
        .setSuccess(true).build());
    } catch (Exception exception) {
      log.processError(exception);
      client.send(requestId, DeploymentScaleResponse.newBuilder()
        .setSuccess(false).build());
    }
  }
}
