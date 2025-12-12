package io.poddeck.agent.resource;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.JSON;
import io.kubernetes.client.util.Yaml;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesApi;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesObject;
import io.kubernetes.client.util.generic.options.CreateOptions;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.ResourceCreateRequest;
import io.poddeck.common.ResourceCreateResponse;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class ResourceCreateService implements Service<ResourceCreateRequest> {
  private final ApiClient apiClient;
  private final Log log;
  private final JSON json = new JSON();

  @Override
  public void process(
    CommunicationClient client, String requestId, ResourceCreateRequest request
  ) throws Exception {
    try {
      var resources = Yaml.loadAll(request.getRaw());
      var created = false;
      for (var resource : resources) {
        if (resource instanceof KubernetesObject) {
          createResource(resource);
          created = true;
        }
      }
      if (!created) {
        throw new IllegalArgumentException("No valid Kubernetes resources found");
      }
      client.send(requestId, ResourceCreateResponse.newBuilder()
        .setSuccess(true).build());
    } catch (Exception exception) {
      log.processError(exception);
      client.send(requestId, ResourceCreateResponse.newBuilder()
        .setSuccess(false).build());
    }
  }

  private void createResource(Object obj) throws Exception {
    if (!(obj instanceof KubernetesObject)) {
      return;
    }
    var dynamic = toDynamicKubernetesObject(obj);
    var groupVersion = extractGroupAndVersion(dynamic.getApiVersion());
    var namespace = getNamespace(dynamic);
    var plural = toPlural(dynamic.getKind());
    var dynamicApi = new DynamicKubernetesApi(groupVersion[0], groupVersion[1],
      plural, apiClient);
    dynamicApi.create(namespace, dynamic, new CreateOptions()).throwsApiException();
  }

  private DynamicKubernetesObject toDynamicKubernetesObject(Object obj) {
    var tree = json.getGson().toJsonTree(obj);
    var asObject = tree.getAsJsonObject();
    return new DynamicKubernetesObject(asObject);
  }

  private String getNamespace(DynamicKubernetesObject dynamic) {
    return dynamic.getMetadata() != null && dynamic.getMetadata().getNamespace() != null
      ? dynamic.getMetadata().getNamespace()
      : "default";
  }

  private String toPlural(String kind) {
    return kind.toLowerCase() + "s";
  }

  private String[] extractGroupAndVersion(String apiVersion) {
    if (apiVersion.contains("/")) {
      var parts = apiVersion.split("/", 2);
      return new String[]{parts[0], parts[1]};
    }
    return new String[]{"", apiVersion};
  }
}
