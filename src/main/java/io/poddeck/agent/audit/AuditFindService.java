package io.poddeck.agent.audit;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1Pod;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.AuditFindRequest;
import io.poddeck.common.AuditFindResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.Comparator;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class AuditFindService implements Service<AuditFindRequest> {
  private final AuditJob auditJob;
  private final CoreV1Api coreV1Api;
  private final AuditFactory auditFactory;

  @Override
  public void process(
    CommunicationClient client, String requestId,
    AuditFindRequest request
  ) throws Exception {
    try {
      var pod = getJobPod(auditJob.job());
      var log = coreV1Api.readNamespacedPodLog(pod.getMetadata().getName(),
        pod.getMetadata().getNamespace()).execute();
      client.send(requestId, AuditFindResponse.newBuilder()
        .setSuccess(true).setAudit(auditFactory.fromJson(log)).build());
    } catch (Exception exception) {
      client.send(requestId, AuditFindResponse.newBuilder()
        .setSuccess(false).build());
    }
  }

  private V1Pod getJobPod(V1Job job) throws Exception {
    var metadata = job.getMetadata();
    var labelSelector = "job-name=" + metadata.getName();
    var pods = coreV1Api.listNamespacedPod(metadata.getNamespace())
      .labelSelector(labelSelector).execute();
    if (pods.getItems().isEmpty()) {
      throw new IllegalStateException("No pod found for job");
    }
    return pods.getItems().stream()
      .max(Comparator.comparing(pod -> pod.getMetadata().getCreationTimestamp()))
      .orElseThrow(() -> new IllegalStateException("No pod found for job"));
  }
}
