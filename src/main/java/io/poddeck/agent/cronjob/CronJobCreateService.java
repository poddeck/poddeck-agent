package io.poddeck.agent.cronjob;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.models.V1CronJob;
import io.kubernetes.client.util.Yaml;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.CronJobCreateRequest;
import io.poddeck.common.CronJobCreateResponse;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class CronJobCreateService implements Service<CronJobCreateRequest> {
  private final BatchV1Api batchV1Api;
  private final Log log;

  @Override
  public void process(
    CommunicationClient client, String requestId,
    CronJobCreateRequest request
  ) throws Exception {
    try {
      var cronJob = Yaml.loadAs(request.getRaw(), V1CronJob.class);
      var metadata = cronJob.getMetadata();
      var namespace = metadata != null && metadata.getNamespace() != null ?
        metadata.getNamespace() : "default";
      batchV1Api.createNamespacedCronJob(namespace, cronJob).execute();
      client.send(requestId, CronJobCreateResponse.newBuilder()
        .setSuccess(true).setNamespace(namespace)
        .setCronJob(metadata != null && metadata.getName() != null ?
          metadata.getName() : "")
        .build());
    } catch (Exception exception) {
      log.processError(exception);
      client.send(requestId, CronJobCreateResponse.newBuilder()
        .setSuccess(false).build());
    }
  }
}
