package io.poddeck.agent.namespace;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.NamespaceDeleteRequest;
import io.poddeck.common.NamespaceDeleteResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class NamespaceDeleteService implements Service<NamespaceDeleteRequest> {
  private final CoreV1Api coreApi;

  @Override
  public void process(
    CommunicationClient client, String requestId,
    NamespaceDeleteRequest namespaceDeleteRequest
  ) throws Exception {
    coreApi.deleteNamespace(namespaceDeleteRequest.getName()).execute();
    client.send(requestId, NamespaceDeleteResponse.newBuilder()
      .setSuccess(true).build());
  }
}
