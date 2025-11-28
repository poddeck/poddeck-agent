package io.poddeck.agent.namespace;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.NamespaceCreateRequest;
import io.poddeck.common.NamespaceCreateResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class NamespaceCreateService implements Service<NamespaceCreateRequest> {
  private final CoreV1Api coreApi;

  @Override
  public void process(
    CommunicationClient client, String requestId,
    NamespaceCreateRequest namespaceCreateRequest
  ) throws Exception {
    var namespace = new V1Namespace().metadata(
      new V1ObjectMeta().name(namespaceCreateRequest.getName()));
    coreApi.createNamespace(namespace).execute();
    client.send(requestId, NamespaceCreateResponse.newBuilder()
      .setSuccess(true).build());
  }
}
