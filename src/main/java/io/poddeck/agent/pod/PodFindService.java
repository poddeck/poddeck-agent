package io.poddeck.agent.pod;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.PodFindRequest;
import io.poddeck.common.PodFindResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class PodFindService implements Service<PodFindRequest> {
  private final CoreV1Api coreApi;
  private final PodFactory podFactory;

  @Override
  public void process(
    CommunicationClient client, String requestId, PodFindRequest request
  ) throws Exception {
    var pod = coreApi.readNamespacedPod(request.getPod(), request.getNamespace())
      .execute();
    client.send(requestId, PodFindResponse.newBuilder()
      .setSuccess(true)
      .setPod(podFactory.assemblePod(pod))
      .build());
  }
}
