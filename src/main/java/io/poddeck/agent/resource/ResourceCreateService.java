package io.poddeck.agent.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.util.Yaml;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesListObject;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesObject;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.ResourceCreateRequest;
import io.poddeck.common.ResourceCreateResponse;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class ResourceCreateService implements Service<ResourceCreateRequest> {
  private final ApiClient apiClient;
  private final Log log;

  @Override
  public void process(
    CommunicationClient client, String requestId,
    ResourceCreateRequest request
  ) throws Exception {
    try {
      CustomObjectsApi api = new CustomObjectsApi(apiClient);

      // 2. Deserialize YAML into a generic Map
      // The Yaml utility from the client library is excellent for this.
      @SuppressWarnings("unchecked")
      Map<String, Object> genericObject = (Map<String, Object>) Yaml.load(request.getRaw());

      if (genericObject == null) {
        System.err.println("Could not parse YAML.");
        return;
      }

      // 3. Extract Details (ApiVersion and Kind are mandatory)
      String apiVersion = (String) genericObject.get("apiVersion");
      String kind = (String) genericObject.get("kind");

      if (apiVersion == null || kind == null) {
        throw new IllegalArgumentException("Resource YAML must contain apiVersion and kind.");
      }

      // --- Determine resource group and scope ---
      String[] parts = apiVersion.split("/");
      String group = parts.length > 1 ? parts[0] : ""; // e.g., 'apps' for 'apps/v1'
      String version = parts.length > 1 ? parts[1] : parts[0]; // e.g., 'v1' for 'apps/v1' or 'v1' for 'v1' core group

      // For simplicity, we'll try to determine the plural name and namespace from the object's metadata.
      // A robust solution would require a lookup against the K8s API discovery endpoint to get the plural name (e.g., 'deployments' for kind 'Deployment').
      // For standard resources, you can often infer it or use a lookup table.
      // For this example, we'll use a placeholder for the plural.
      String pluralName = kind.toLowerCase() + "s"; // **WARNING: This is a weak inference!**

      @SuppressWarnings("unchecked")
      Map<String, Object> metadata = (Map<String, Object>) genericObject.get("metadata");
      String namespace = (metadata != null) ? (String) metadata.get("namespace") : null;
      String name = (metadata != null) ? (String) metadata.get("name") : null;


      // 5. Create Resource using CustomObjectsApi
      Object createdObject;

      if (namespace != null) {
        // Namespaced Resource
        System.out.println("Creating namespaced resource: " + kind + "/" + name + " in namespace " + namespace);
        createdObject = api.createNamespacedCustomObject(
          group,       // The API group (e.g., "apps")
          version,     // The API version (e.g., "v1")
          namespace,   // The namespace
          pluralName,  // The plural resource name (e.g., "deployments")
          genericObject
        );
      } else {
        // Cluster-scoped Resource (e.g., Namespace, ClusterRole, CRD)
        System.out.println("Creating cluster-scoped resource: " + kind + "/" + name);
        createdObject = api.createClusterCustomObject(
          group,       // The API group (e.g., "apiextensions.k8s.io")
          version,     // The API version (e.g., "v1")
          pluralName,  // The plural resource name (e.g., "customresourcedefinitions")
          genericObject
        );
      }
      client.send(requestId, ResourceCreateResponse.newBuilder()
        .setSuccess(true).build());
    } catch (Exception e) {
      log.processError(e);
      client.send(requestId, ResourceCreateResponse.newBuilder()
        .setSuccess(false).build());
    }
  }

  private GenericKubernetesApi<DynamicKubernetesObject, DynamicKubernetesListObject> genericKubernetesApi(
    DynamicKubernetesObject resource
  ) throws Exception {
    var apiVersion = resource.getApiVersion();
    var kind = resource.getKind();
    if (apiVersion == null || kind == null) {
      throw new Exception("YAML missing apiVersion or kind");
    }
    var group = "";
    var version = apiVersion;
    if (apiVersion.contains("/")) {
      var parts = apiVersion.split("/", 2);
      group = parts[0];
      version = parts[1];
    }
    var plural = kind.toLowerCase() + "s";
    return new GenericKubernetesApi<>(DynamicKubernetesObject.class,
      DynamicKubernetesListObject.class, group, version, plural, apiClient);
  }
}
