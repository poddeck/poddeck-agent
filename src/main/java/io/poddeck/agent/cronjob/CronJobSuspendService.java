package io.poddeck.agent.cronjob;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.models.V1CronJob;
import io.kubernetes.client.util.PatchUtils;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.CronJobSuspendRequest;
import io.poddeck.common.CronJobSuspendResponse;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class CronJobSuspendService implements Service<CronJobSuspendRequest> {
  private final BatchV1Api batchV1Api;
  private final Log log;

  @Override
  public void process(
    CommunicationClient client, String requestId,
    CronJobSuspendRequest request
  ) throws Exception {
    try {
      var patchJson = "{\"spec\":{\"suspend\":" + request.getSuspend() + "}}";
      PatchUtils.patch(V1CronJob.class,
        () -> batchV1Api.patchNamespacedCronJob(request.getCronJob(),
            request.getNamespace(), new V1Patch(patchJson))
          .buildCall(null),
        V1Patch.PATCH_FORMAT_JSON_MERGE_PATCH,
        batchV1Api.getApiClient());
      client.send(requestId, CronJobSuspendResponse.newBuilder()
        .setSuccess(true).build());
    } catch (Exception exception) {
      log.processError(exception);
      client.send(requestId, CronJobSuspendResponse.newBuilder()
        .setSuccess(false).build());
    }
  }
}
