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
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class NamespaceCreateService implements Service<NamespaceCreateRequest> {
  private final CoreV1Api coreApi;
  private final Log log;

  @Override
  public void process(
    CommunicationClient client, String requestId, NamespaceCreateRequest request
  ) throws Exception {
    try {
      var namespace = new V1Namespace().metadata(
        new V1ObjectMeta().name(request.getName()));
      coreApi.createNamespace(namespace).execute();
      client.send(requestId, NamespaceCreateResponse.newBuilder()
        .setSuccess(true).build());
    } catch (Exception exception) {
      log.processError(exception);
      client.send(requestId, NamespaceCreateResponse.newBuilder()
        .setSuccess(false).build());
    }
  }
}
