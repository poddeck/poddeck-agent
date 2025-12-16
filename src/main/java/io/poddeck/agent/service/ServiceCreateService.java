package io.poddeck.agent.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.util.Yaml;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.ServiceCreateRequest;
import io.poddeck.common.ServiceCreateResponse;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class ServiceCreateService implements Service<ServiceCreateRequest> {
  private final CoreV1Api coreApi;
  private final Log log;

  @Override
  public void process(
    CommunicationClient client, String requestId,
    ServiceCreateRequest request
  ) throws Exception {
    try {
      var service = Yaml.loadAs(request.getRaw(), V1Service.class);
      var metadata = service.getMetadata();
      var namespace = metadata != null && metadata.getNamespace() != null ?
        metadata.getNamespace() : "default";
      coreApi.createNamespacedService(namespace, service).execute();
      client.send(requestId, ServiceCreateResponse.newBuilder()
        .setSuccess(true).setNamespace(namespace)
        .setService(metadata != null && metadata.getName() != null ?
          metadata.getName() : "")
        .build());
    } catch (Exception exception) {
      log.processError(exception);
      client.send(requestId, ServiceCreateResponse.newBuilder()
        .setSuccess(false).build());
    }
  }
}
