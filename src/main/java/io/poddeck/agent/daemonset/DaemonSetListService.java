package io.poddeck.agent.daemonset;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.DaemonSetListRequest;
import io.poddeck.common.DaemonSetListResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class DaemonSetListService implements Service<DaemonSetListRequest> {
  private final AppsV1Api appsApi;
  private final DaemonSetFactory daemonSetFactory;

  @Override
  public void process(
    CommunicationClient client, String requestId, DaemonSetListRequest request
  ) throws Exception {
    var daemonSetList = appsApi.listDaemonSetForAllNamespaces().execute().getItems();
    var daemonSets = daemonSetList.stream()
      .map(daemonSetFactory::assembleDaemonSet).toList();
    client.send(requestId, DaemonSetListResponse.newBuilder()
      .addAllItems(daemonSets)
      .build());
  }
}
