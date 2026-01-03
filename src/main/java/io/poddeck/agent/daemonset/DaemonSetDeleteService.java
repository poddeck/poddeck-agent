package io.poddeck.agent.daemonset;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.DaemonSetDeleteRequest;
import io.poddeck.common.DaemonSetDeleteResponse;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class DaemonSetDeleteService implements Service<DaemonSetDeleteRequest> {
  private final AppsV1Api appsApi;
  private final Log log;

  @Override
  public void process(
    CommunicationClient client, String requestId,
    DaemonSetDeleteRequest request
  ) throws Exception {
    try {
      appsApi.deleteNamespacedDaemonSet(request.getDaemonSet(),
        request.getNamespace()).execute();
      client.send(requestId, DaemonSetDeleteResponse.newBuilder()
        .setSuccess(true).build());
    } catch (Exception exception) {
      log.processError(exception);
      client.send(requestId, DaemonSetDeleteResponse.newBuilder()
        .setSuccess(false).build());
    }
  }
}
