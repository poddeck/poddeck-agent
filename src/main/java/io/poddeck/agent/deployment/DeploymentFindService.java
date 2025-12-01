package io.poddeck.agent.deployment;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.DeploymentFindRequest;
import io.poddeck.common.DeploymentFindResponse;
import io.poddeck.common.PodFindRequest;
import io.poddeck.common.PodFindResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class DeploymentFindService implements Service<DeploymentFindRequest> {
  private final AppsV1Api appsApi;
  private final DeploymentFactory deploymentFactory;

  @Override
  public void process(
    CommunicationClient client, String requestId, DeploymentFindRequest request
  ) throws Exception {
    var deployment = appsApi.readNamespacedDeployment(request.getDeployment(),
      request.getNamespace()).execute();
    client.send(requestId, DeploymentFindResponse.newBuilder()
      .setSuccess(true)
      .setDeployment(deploymentFactory.assembleDeployment(deployment))
      .build());
  }
}
