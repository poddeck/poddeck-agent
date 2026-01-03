package io.poddeck.agent.statefulset;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.StatefulSetListRequest;
import io.poddeck.common.StatefulSetListResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class StatefulSetListService implements Service<StatefulSetListRequest> {
  private final AppsV1Api appsApi;
  private final StatefulSetFactory statefulSetFactory;

  @Override
  public void process(
    CommunicationClient client, String requestId, StatefulSetListRequest request
  ) throws Exception {
    var statefulSetList = appsApi.listStatefulSetForAllNamespaces().execute().getItems();
    var statefulSets = statefulSetList.stream()
      .map(statefulSetFactory::assembleStatefulSet).toList();
    client.send(requestId, StatefulSetListResponse.newBuilder()
      .addAllItems(statefulSets)
      .build());
  }
}
