package io.poddeck.agent.statefulset;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import io.kubernetes.client.util.PatchUtils;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.StatefulSetRestartRequest;
import io.poddeck.common.StatefulSetRestartResponse;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class StatefulSetRestartService implements Service<StatefulSetRestartRequest> {
  private final AppsV1Api appsApi;
  private final Log log;

  @Override
  public void process(
    CommunicationClient client, String requestId,
    StatefulSetRestartRequest request
  ) throws Exception {
    try {
      var patchJson = "{ \"spec\": { \"template\": { \"metadata\": " +
        "{ \"annotations\": {\"kubectl.kubernetes.io/restartedAt\": \"" +
        java.time.Instant.now() + "\" }}}}}";
      PatchUtils.patch(V1StatefulSet.class,
        () -> appsApi.patchNamespacedStatefulSet(request.getStatefulSet(),
            request.getNamespace(), new V1Patch(patchJson))
          .buildCall(null),
        V1Patch.PATCH_FORMAT_JSON_MERGE_PATCH,
        appsApi.getApiClient());
      client.send(requestId, StatefulSetRestartResponse.newBuilder()
        .setSuccess(true).build());
    } catch (Exception exception) {
      log.processError(exception);
      client.send(requestId, StatefulSetRestartResponse.newBuilder()
        .setSuccess(false).build());
    }
  }
}
