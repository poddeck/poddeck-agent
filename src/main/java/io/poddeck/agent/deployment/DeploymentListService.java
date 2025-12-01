package io.poddeck.agent.deployment;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.DeploymentListRequest;
import io.poddeck.common.DeploymentListResponse;
import io.poddeck.common.PodListRequest;
import io.poddeck.common.PodListResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class DeploymentListService implements Service<DeploymentListRequest> {
  private final AppsV1Api appsApi;
  private final DeploymentFactory deploymentFactory;

  @Override
  public void process(
    CommunicationClient client, String requestId, DeploymentListRequest request
  ) throws Exception {
    var deploymentList = appsApi.listDeploymentForAllNamespaces().execute().getItems();
    var deployments = deploymentList.stream()
      .map(deploymentFactory::assembleDeployment).toList();
    client.send(requestId, DeploymentListResponse.newBuilder()
      .addAllItems(deployments)
      .build());
  }
}
