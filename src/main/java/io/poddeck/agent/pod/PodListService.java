package io.poddeck.agent.pod;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.PodListRequest;
import io.poddeck.common.PodListResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class PodListService implements Service<PodListRequest> {
  private final CoreV1Api coreApi;
  private final PodFactory podFactory;

  @Override
  public void process(
    CommunicationClient client, String requestId, PodListRequest request
  ) throws Exception {
    var podList = coreApi.listPodForAllNamespaces().execute().getItems();
    var pods = podList.stream().map(podFactory::assemblePod).toList();
    client.send(requestId, PodListResponse.newBuilder()
      .addAllItems(pods)
      .build());
  }
}
