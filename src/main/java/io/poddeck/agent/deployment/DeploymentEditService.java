package io.poddeck.agent.deployment;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.util.Yaml;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.DeploymentEditRequest;
import io.poddeck.common.DeploymentEditResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class DeploymentEditService implements Service<DeploymentEditRequest> {
  private final AppsV1Api appsApi;

  @Override
  public void process(
    CommunicationClient client, String requestId,
    DeploymentEditRequest request
  ) throws Exception {
    var deployment = Yaml.loadAs(request.getRaw(), V1Deployment.class);
    appsApi.replaceNamespacedDeployment(request.getDeployment(),
      request.getNamespace(), deployment).execute();
    client.send(requestId, DeploymentEditResponse.newBuilder()
      .setSuccess(true).build());
  }
}
