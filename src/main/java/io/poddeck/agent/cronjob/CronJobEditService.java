package io.poddeck.agent.cronjob;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.models.V1CronJob;
import io.kubernetes.client.util.Yaml;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.CronJobEditRequest;
import io.poddeck.common.CronJobEditResponse;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class CronJobEditService implements Service<CronJobEditRequest> {
  private final BatchV1Api batchV1Api;
  private final Log log;

  @Override
  public void process(
    CommunicationClient client, String requestId,
    CronJobEditRequest request
  ) throws Exception {
    try {
      var cronJob = Yaml.loadAs(request.getRaw(), V1CronJob.class);
      batchV1Api.replaceNamespacedCronJob(request.getCronJob(),
        request.getNamespace(), cronJob).execute();
      client.send(requestId, CronJobEditResponse.newBuilder()
        .setSuccess(true).build());
    } catch (Exception exception) {
      log.processError(exception);
      client.send(requestId, CronJobEditResponse.newBuilder()
        .setSuccess(false).build());
    }
  }
}
