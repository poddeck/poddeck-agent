package io.poddeck.agent.audit;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1Pod;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.communication.service.Service;
import io.poddeck.common.AuditPerformRequest;
import io.poddeck.common.AuditPerformResponse;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.Comparator;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class AuditPerformService implements Service<AuditPerformRequest> {
  private final AuditJob auditJob;
  private final BatchV1Api batchV1Api;
  private final CoreV1Api coreV1Api;
  private final Log log;
  private final AuditFactory auditFactory;

  @Override
  public void process(
    CommunicationClient client, String requestId,
    AuditPerformRequest request
  ) throws Exception {
    deleteJob(auditJob.job());
    V1Job createdJob = null;
    try {
      createdJob = batchV1Api.createNamespacedJob(
        auditJob.job().getMetadata().getNamespace(), auditJob.job()).execute();
      waitForJobCompletion(createdJob);
      var pod = getJobPod(createdJob);
      var log = coreV1Api.readNamespacedPodLog(pod.getMetadata().getName(),
        pod.getMetadata().getNamespace()).execute();
      client.send(requestId, AuditPerformResponse.newBuilder()
        .setSuccess(true).setAudit(auditFactory.fromJson(log)).build());
    } catch (Exception exception) {
      log.processError(exception);
      client.send(requestId, AuditPerformResponse.newBuilder()
        .setSuccess(false).build());
    }
  }

  private static final long TIMEOUT = 60000L;

  private void waitForJobCompletion(V1Job job) throws Exception {
    long start = System.currentTimeMillis();
    var metadata = job.getMetadata();
    while (System.currentTimeMillis() - start < TIMEOUT) {
      var current = batchV1Api.readNamespacedJob(metadata.getName(),
        metadata.getNamespace()).execute();
      var status = current.getStatus();
      if (status != null) {
        if (status.getSucceeded() != null && status.getSucceeded() > 0) {
          return;
        }
        if (status.getFailed() != null && status.getFailed() > 0) {
          throw new IllegalStateException("Job failed");
        }
      }
      Thread.sleep(1000);
    }
    throw new IllegalStateException("Timeout waiting for Job completion");
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

  private void deleteJob(V1Job job) {
    if (job == null) {
      return;
    }
    try {
      var pod = getJobPod(job);
      coreV1Api.deleteNamespacedPod(pod.getMetadata().getName(),
        pod.getMetadata().getNamespace()).execute();
    } catch (Exception exception) {
    }
    try {
      batchV1Api.deleteNamespacedJob(job.getMetadata().getName(),
        job.getMetadata().getNamespace()).execute();
    } catch (Exception exception) {
    }
  }
}
