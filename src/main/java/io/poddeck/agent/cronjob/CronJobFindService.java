package io.poddeck.agent.cronjob;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.CronJobFindRequest;
import io.poddeck.common.CronJobFindResponse;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class CronJobFindService implements Service<CronJobFindRequest> {
  private final BatchV1Api batchV1Api;
  private final CronJobFactory cronJobFactory;
  private final Log log;

  @Override
  public void process(
    CommunicationClient client, String requestId, CronJobFindRequest request
  ) throws Exception {
    try {
      var cronJob = batchV1Api.readNamespacedCronJob(request.getCronJob(),
        request.getNamespace()).execute();
      client.send(requestId, CronJobFindResponse.newBuilder()
        .setSuccess(true)
        .setCronJob(cronJobFactory.assembleCronJob(cronJob))
        .build());
    } catch (Exception exception) {
      log.processError(exception);
      client.send(requestId, CronJobFindResponse.newBuilder()
        .setSuccess(false).build());
    }
  }
}
