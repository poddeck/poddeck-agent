package io.poddeck.agent.pod;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.PodLogRequest;
import io.poddeck.common.PodLogResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class PodLogService implements Service<PodLogRequest> {
  private final CoreV1Api coreApi;

  @Override
  public void process(
    CommunicationClient client, String requestId, PodLogRequest request
  ) throws Exception {
    var builder = coreApi
      .readNamespacedPodLog(request.getPod(), request.getNamespace())
      .timestamps(true);
    if (!request.getContainer().isEmpty()) {
      builder = builder.container(request.getContainer());
    }
    if (request.getSinceSeconds() > 0) {
      builder = builder.sinceSeconds(request.getSinceSeconds());
    }
    var logs = builder.execute();
    if (logs == null) {
      logs = "";
    }
    client.send(requestId, PodLogResponse.newBuilder()
      .setLogs(logs).build());
  }
}
