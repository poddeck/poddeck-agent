package io.poddeck.agent.deployment;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.util.Yaml;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.DeploymentCreateRequest;
import io.poddeck.common.DeploymentCreateResponse;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class DeploymentCreateService implements Service<DeploymentCreateRequest> {
  private final AppsV1Api appsApi;
  private final Log log;

  @Override
  public void process(
    CommunicationClient client, String requestId,
    DeploymentCreateRequest request
  ) throws Exception {
    try {
      var deployment = Yaml.loadAs(request.getRaw(), V1Deployment.class);
      var metadata = deployment.getMetadata();
      var namespace = metadata != null && metadata.getNamespace() != null ?
        metadata.getNamespace() : "default";
      appsApi.createNamespacedDeployment(namespace, deployment).execute();
      client.send(requestId, DeploymentCreateResponse.newBuilder()
        .setSuccess(true).setNamespace(namespace)
        .setDeployment(metadata != null && metadata.getName() != null ?
          metadata.getName() : "")
        .build());
    } catch (Exception exception) {
      log.processError(exception);
      client.send(requestId, DeploymentCreateResponse.newBuilder()
        .setSuccess(false).build());
    }
  }
}
