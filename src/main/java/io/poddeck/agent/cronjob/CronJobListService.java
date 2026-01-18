package io.poddeck.agent.cronjob;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.CronJobListRequest;
import io.poddeck.common.CronJobListResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class CronJobListService implements Service<CronJobListRequest> {
  private final BatchV1Api batchV1Api;
  private final CronJobFactory cronJobFactory;

  @Override
  public void process(
    CommunicationClient client, String requestId, CronJobListRequest request
  ) throws Exception {
    var cronJobList = batchV1Api.listCronJobForAllNamespaces().execute().getItems();
    var cronJobs = cronJobList.stream()
      .map(cronJobFactory::assembleCronJob).toList();
    client.send(requestId, CronJobListResponse.newBuilder()
      .addAllItems(cronJobs)
      .build());
  }
}
