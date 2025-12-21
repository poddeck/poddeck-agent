package io.poddeck.agent.deployment;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.DeploymentDeleteRequest;
import io.poddeck.common.DeploymentDeleteResponse;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class DeploymentDeleteService implements Service<DeploymentDeleteRequest> {
  private final AppsV1Api appsApi;
  private final Log log;

  @Override
  public void process(
    CommunicationClient client, String requestId,
    DeploymentDeleteRequest request
  ) throws Exception {
    try {
      appsApi.deleteNamespacedDeployment(request.getDeployment(),
        request.getNamespace()).execute();
      client.send(requestId, DeploymentDeleteResponse.newBuilder()
        .setSuccess(true).build());
    } catch (Exception exception) {
      log.processError(exception);
      client.send(requestId, DeploymentDeleteResponse.newBuilder()
        .setSuccess(false).build());
    }
  }
}
