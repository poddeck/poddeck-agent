package io.poddeck.agent.statefulset;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import io.kubernetes.client.util.Yaml;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.StatefulSetEditRequest;
import io.poddeck.common.StatefulSetEditResponse;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class StatefulSetEditService implements Service<StatefulSetEditRequest> {
  private final AppsV1Api appsApi;
  private final Log log;

  @Override
  public void process(
    CommunicationClient client, String requestId,
    StatefulSetEditRequest request
  ) throws Exception {
    try {
      var statefulSet = Yaml.loadAs(request.getRaw(), V1StatefulSet.class);
      appsApi.replaceNamespacedStatefulSet(request.getStatefulSet(),
        request.getNamespace(), statefulSet).execute();
      client.send(requestId, StatefulSetEditResponse.newBuilder()
        .setSuccess(true).build());
    } catch (Exception exception) {
      log.processError(exception);
      client.send(requestId, StatefulSetEditResponse.newBuilder()
        .setSuccess(false).build());
    }
  }
}
