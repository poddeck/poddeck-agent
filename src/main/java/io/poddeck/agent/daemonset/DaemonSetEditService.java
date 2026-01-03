package io.poddeck.agent.daemonset;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1DaemonSet;
import io.kubernetes.client.util.Yaml;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.DaemonSetEditRequest;
import io.poddeck.common.DaemonSetEditResponse;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class DaemonSetEditService implements Service<DaemonSetEditRequest> {
  private final AppsV1Api appsApi;
  private final Log log;

  @Override
  public void process(
    CommunicationClient client, String requestId,
    DaemonSetEditRequest request
  ) throws Exception {
    try {
      var daemonSet = Yaml.loadAs(request.getRaw(), V1DaemonSet.class);
      appsApi.replaceNamespacedDaemonSet(request.getDaemonSet(),
        request.getNamespace(), daemonSet).execute();
      client.send(requestId, DaemonSetEditResponse.newBuilder()
        .setSuccess(true).build());
    } catch (Exception exception) {
      log.processError(exception);
      client.send(requestId, DaemonSetEditResponse.newBuilder()
        .setSuccess(false).build());
    }
  }
}
