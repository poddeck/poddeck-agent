package io.poddeck.agent.pod;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.PodDeleteRequest;
import io.poddeck.common.PodDeleteResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class PodDeleteService implements Service<PodDeleteRequest> {
  private final CoreV1Api coreApi;

  @Override
  public void process(
    CommunicationClient client, String requestId,
    PodDeleteRequest podDeleteRequest
  ) throws Exception {
    coreApi.deleteNamespacedPod(podDeleteRequest.getPod(),
      podDeleteRequest.getNamespace()).execute();
    client.send(requestId, PodDeleteResponse.newBuilder()
      .setSuccess(true).build());
  }
}
