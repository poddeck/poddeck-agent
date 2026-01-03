package io.poddeck.agent.statefulset;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import io.kubernetes.client.util.Yaml;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.StatefulSetCreateRequest;
import io.poddeck.common.StatefulSetCreateResponse;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class StatefulSetCreateService implements Service<StatefulSetCreateRequest> {
  private final AppsV1Api appsApi;
  private final Log log;

  @Override
  public void process(
    CommunicationClient client, String requestId,
    StatefulSetCreateRequest request
  ) throws Exception {
    try {
      var statefulSet = Yaml.loadAs(request.getRaw(), V1StatefulSet.class);
      var metadata = statefulSet.getMetadata();
      var namespace = metadata != null && metadata.getNamespace() != null ?
        metadata.getNamespace() : "default";
      appsApi.createNamespacedStatefulSet(namespace, statefulSet).execute();
      client.send(requestId, StatefulSetCreateResponse.newBuilder()
        .setSuccess(true).setNamespace(namespace)
        .setStatefulSet(metadata != null && metadata.getName() != null ?
          metadata.getName() : "")
        .build());
    } catch (Exception exception) {
      log.processError(exception);
      client.send(requestId, StatefulSetCreateResponse.newBuilder()
        .setSuccess(false).build());
    }
  }
}
