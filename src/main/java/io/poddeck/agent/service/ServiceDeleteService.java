package io.poddeck.agent.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.ServiceDeleteRequest;
import io.poddeck.common.ServiceDeleteResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class ServiceDeleteService implements Service<ServiceDeleteRequest> {
  private final CoreV1Api coreApi;

  @Override
  public void process(
    CommunicationClient client, String requestId,
    ServiceDeleteRequest request
  ) throws Exception {
    coreApi.deleteNamespacedService(request.getService(),
      request.getNamespace()).execute();
    client.send(requestId, ServiceDeleteResponse.newBuilder()
      .setSuccess(true).build());
  }
}
