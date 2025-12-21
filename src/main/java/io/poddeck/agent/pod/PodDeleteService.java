package io.poddeck.agent.pod;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.PodDeleteRequest;
import io.poddeck.common.PodDeleteResponse;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class PodDeleteService implements Service<PodDeleteRequest> {
  private final CoreV1Api coreApi;
  private final Log log;

  @Override
  public void process(
    CommunicationClient client, String requestId, PodDeleteRequest request
  ) throws Exception {
    try {
      coreApi.deleteNamespacedPod(request.getPod(), request.getNamespace())
        .execute();
      client.send(requestId, PodDeleteResponse.newBuilder()
        .setSuccess(true).build());
    } catch (Exception exception) {
      log.processError(exception);
      client.send(requestId, PodDeleteResponse.newBuilder()
        .setSuccess(false).build());
    }
  }
}
