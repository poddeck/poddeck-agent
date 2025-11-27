package io.poddeck.agent.namespace;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.Namespace;
import io.poddeck.common.NamespaceListRequest;
import io.poddeck.common.NamespaceListResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class NamespaceListService implements Service<NamespaceListRequest> {
  private final CoreV1Api coreApi;

  @Override
  public void process(
    CommunicationClient client, String requestId,
    NamespaceListRequest namespaceListRequest
  ) throws Exception {
    var nodes = coreApi.listNamespace().execute().getItems()
      .stream().map(this::assembleNamespace).toList();
    client.send(requestId, NamespaceListResponse.newBuilder()
      .addAllItems(nodes).build());
  }

  private Namespace assembleNamespace(V1Namespace namespace) {
    var age = 0L;
    if (namespace.getMetadata() != null &&
      namespace.getMetadata().getCreationTimestamp() != null
    ) {
      age = (System.currentTimeMillis() -
        namespace.getMetadata().getCreationTimestamp().toEpochSecond() * 1000);
    }
    return Namespace.newBuilder()
      .setName(namespace.getMetadata().getName())
      .setStatus(namespace.getStatus().getPhase())
      .setAge(age)
      .build();
  }
}