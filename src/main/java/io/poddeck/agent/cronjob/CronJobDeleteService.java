package io.poddeck.agent.cronjob;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.CronJobDeleteRequest;
import io.poddeck.common.CronJobDeleteResponse;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class CronJobDeleteService implements Service<CronJobDeleteRequest> {
  private final BatchV1Api batchV1Api;
  private final Log log;

  @Override
  public void process(
    CommunicationClient client, String requestId,
    CronJobDeleteRequest request
  ) throws Exception {
    try {
      batchV1Api.deleteNamespacedCronJob(request.getCronJob(),
        request.getNamespace()).execute();
      client.send(requestId, CronJobDeleteResponse.newBuilder()
        .setSuccess(true).build());
    } catch (Exception exception) {
      log.processError(exception);
      client.send(requestId, CronJobDeleteResponse.newBuilder()
        .setSuccess(false).build());
    }
  }
}
