package io.poddeck.agent.daemonset;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.DaemonSetFindRequest;
import io.poddeck.common.DaemonSetFindResponse;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class DaemonSetFindService implements Service<DaemonSetFindRequest> {
  private final AppsV1Api appsApi;
  private final DaemonSetFactory daemonSetFactory;
  private final Log log;

  @Override
  public void process(
    CommunicationClient client, String requestId, DaemonSetFindRequest request
  ) throws Exception {
    try {
      var daemonSet = appsApi.readNamespacedDaemonSet(request.getDaemonSet(),
        request.getNamespace()).execute();
      client.send(requestId, DaemonSetFindResponse.newBuilder()
        .setSuccess(true)
        .setDaemonSet(daemonSetFactory.assembleDaemonSet(daemonSet))
        .build());
    } catch (Exception exception) {
      log.processError(exception);
      client.send(requestId, DaemonSetFindResponse.newBuilder()
        .setSuccess(false).build());
    }
  }
}
