package io.poddeck.agent.namespace;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.NamespaceDeleteRequest;
import io.poddeck.common.NamespaceDeleteResponse;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class NamespaceDeleteService implements Service<NamespaceDeleteRequest> {
  private final CoreV1Api coreApi;
  private final Log log;

  @Override
  public void process(
    CommunicationClient client, String requestId, NamespaceDeleteRequest request
  ) throws Exception {
    try {
      coreApi.deleteNamespace(request.getName()).execute();
      client.send(requestId, NamespaceDeleteResponse.newBuilder()
        .setSuccess(true).build());
    } catch (Exception exception) {
      log.processError(exception);
      client.send(requestId, NamespaceDeleteResponse.newBuilder()
        .setSuccess(false).build());
    }
  }
}
