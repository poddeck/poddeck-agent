package io.poddeck.agent.daemonset;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1DaemonSet;
import io.kubernetes.client.util.Yaml;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.DaemonSetCreateRequest;
import io.poddeck.common.DaemonSetCreateResponse;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class DaemonSetCreateService implements Service<DaemonSetCreateRequest> {
  private final AppsV1Api appsApi;
  private final Log log;

  @Override
  public void process(
    CommunicationClient client, String requestId,
    DaemonSetCreateRequest request
  ) throws Exception {
    try {
      var daemonSet = Yaml.loadAs(request.getRaw(), V1DaemonSet.class);
      var metadata = daemonSet.getMetadata();
      var namespace = metadata != null && metadata.getNamespace() != null ?
        metadata.getNamespace() : "default";
      appsApi.createNamespacedDaemonSet(namespace, daemonSet).execute();
      client.send(requestId, DaemonSetCreateResponse.newBuilder()
        .setSuccess(true).setNamespace(namespace)
        .setDaemonSet(metadata != null && metadata.getName() != null ?
          metadata.getName() : "")
        .build());
    } catch (Exception exception) {
      log.processError(exception);
      client.send(requestId, DaemonSetCreateResponse.newBuilder()
        .setSuccess(false).build());
    }
  }
}
