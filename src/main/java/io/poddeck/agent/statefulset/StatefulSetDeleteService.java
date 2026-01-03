package io.poddeck.agent.statefulset;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.StatefulSetDeleteRequest;
import io.poddeck.common.StatefulSetDeleteResponse;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class StatefulSetDeleteService implements Service<StatefulSetDeleteRequest> {
  private final AppsV1Api appsApi;
  private final Log log;

  @Override
  public void process(
    CommunicationClient client, String requestId,
    StatefulSetDeleteRequest request
  ) throws Exception {
    try {
      appsApi.deleteNamespacedStatefulSet(request.getStatefulSet(),
        request.getNamespace()).execute();
      client.send(requestId, StatefulSetDeleteResponse.newBuilder()
        .setSuccess(true).build());
    } catch (Exception exception) {
      log.processError(exception);
      client.send(requestId, StatefulSetDeleteResponse.newBuilder()
        .setSuccess(false).build());
    }
  }
}
