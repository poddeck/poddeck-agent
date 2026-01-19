package io.poddeck.agent.cronjob;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.CronJobRunRequest;
import io.poddeck.common.CronJobRunResponse;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class CronJobRunService implements Service<CronJobRunRequest> {
  private final BatchV1Api batchV1Api;
  private final Log log;

  @Override
  public void process(
    CommunicationClient client, String requestId,
    CronJobRunRequest request
  ) throws Exception {
    try {
      var cronJob = batchV1Api.readNamespacedCronJob(request.getCronJob(),
        request.getNamespace()).execute();
      var jobName = request.getCronJob() + "-manual-" + System.currentTimeMillis();
      var job = new V1Job()
        .metadata(new V1ObjectMeta().name(jobName)
          .namespace(request.getNamespace())
          .putLabelsItem("trigger", "manual")
          .putLabelsItem("cronjob", request.getCronJob()))
        .spec(cronJob.getSpec().getJobTemplate().getSpec());
      var createdJob = batchV1Api.createNamespacedJob(
        request.getNamespace(), job).execute();
      client.send(requestId, CronJobRunResponse.newBuilder()
        .setSuccess(true).build());
    } catch (Exception exception) {
      log.processError(exception);
      client.send(requestId, CronJobRunResponse.newBuilder()
        .setSuccess(false).build());
    }
  }
}
