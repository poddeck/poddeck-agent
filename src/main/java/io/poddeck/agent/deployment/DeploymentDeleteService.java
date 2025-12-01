package io.poddeck.agent.deployment;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.DeploymentDeleteRequest;
import io.poddeck.common.DeploymentDeleteResponse;
import io.poddeck.common.PodDeleteRequest;
import io.poddeck.common.PodDeleteResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class DeploymentDeleteService implements Service<DeploymentDeleteRequest> {
  private final AppsV1Api appsApi;

  @Override
  public void process(
    CommunicationClient client, String requestId,
    DeploymentDeleteRequest request
  ) throws Exception {
    appsApi.deleteNamespacedDeployment(request.getDeployment(),
      request.getNamespace()).execute();
    client.send(requestId, DeploymentDeleteResponse.newBuilder()
      .setSuccess(true).build());
  }
}
