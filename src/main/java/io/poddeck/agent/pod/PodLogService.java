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
    var namespace = request.getNamespace();
    var pod = request.getPod();
    var container = request.getContainer().isEmpty() ?
      null : request.getContainer();
    int tailLines = request.getTailLines() > 0 ?
      request.getTailLines() : null;
    int sinceSeconds = request.getSinceSeconds() > 0 ?
      request.getSinceSeconds() : null;
    var logs = coreApi.readNamespacedPodLog(pod, namespace)
      .container(container).tailLines(tailLines)
      .sinceSeconds(sinceSeconds)
      .timestamps(true)
      .execute();
    client.send(requestId, PodLogResponse.newBuilder()
      .setLogs(logs).build());
  }
}
