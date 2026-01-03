package io.poddeck.agent.statefulset;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.StatefulSetFindRequest;
import io.poddeck.common.StatefulSetFindResponse;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class StatefulSetFindService implements Service<StatefulSetFindRequest> {
  private final AppsV1Api appsApi;
  private final StatefulSetFactory statefulSetFactory;
  private final Log log;

  @Override
  public void process(
    CommunicationClient client, String requestId, StatefulSetFindRequest request
  ) throws Exception {
    try {
      var statefulSet = appsApi.readNamespacedStatefulSet(request.getStatefulSet(),
        request.getNamespace()).execute();
      client.send(requestId, StatefulSetFindResponse.newBuilder()
        .setSuccess(true)
        .setStatefulSet(statefulSetFactory.assembleStatefulSet(statefulSet))
        .build());
    } catch (Exception exception) {
      log.processError(exception);
      client.send(requestId, StatefulSetFindResponse.newBuilder()
        .setSuccess(false).build());
    }
  }
}
