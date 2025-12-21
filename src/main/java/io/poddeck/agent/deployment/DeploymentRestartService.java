package io.poddeck.agent.deployment;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.util.PatchUtils;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.DeploymentRestartRequest;
import io.poddeck.common.DeploymentRestartResponse;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class DeploymentRestartService implements Service<DeploymentRestartRequest> {
  private final AppsV1Api appsApi;
  private final Log log;

  @Override
  public void process(
    CommunicationClient client, String requestId,
    DeploymentRestartRequest request
  ) throws Exception {
    try {
      var patchJson = "{ \"spec\": { \"template\": { \"metadata\": " +
        "{ \"annotations\": {\"kubectl.kubernetes.io/restartedAt\": \"" +
        java.time.Instant.now() + "\" }}}}}";
      PatchUtils.patch(V1Deployment.class,
        () -> appsApi.patchNamespacedDeployment(request.getDeployment(),
            request.getNamespace(), new V1Patch(patchJson))
          .buildCall(null),
        V1Patch.PATCH_FORMAT_JSON_MERGE_PATCH,
        appsApi.getApiClient());
      client.send(requestId, DeploymentRestartResponse.newBuilder()
        .setSuccess(true).build());
    } catch (Exception exception) {
      log.processError(exception);
      client.send(requestId, DeploymentRestartResponse.newBuilder()
        .setSuccess(false).build());
    }
  }
}
