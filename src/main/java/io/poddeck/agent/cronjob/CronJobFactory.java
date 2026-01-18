package io.poddeck.agent.cronjob;

import com.google.api.client.util.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Yaml;
import io.poddeck.agent.pod.PodFactory;
import io.poddeck.common.*;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class CronJobFactory {
  private final CoreV1Api coreApi;
  private final PodFactory podFactory;
  private final Log log;

  public CronJob assembleCronJob(V1CronJob cronJob) {
    return CronJob.newBuilder()
      .setMetadata(assembleMetadata(cronJob))
      .setSpec(assembleSpec(cronJob))
      .setStatus(assembleStatus(cronJob))
      .addAllEvents(assembleCronJobEvents(cronJob))
      .setRaw(Yaml.dump(cronJob))
      .build();
  }

  private CronJobMetadata assembleMetadata(V1CronJob cronJob) {
    if (cronJob.getMetadata() == null) {
      return CronJobMetadata.newBuilder().build();
    }
    var metadata = cronJob.getMetadata();
    return CronJobMetadata.newBuilder()
      .setName(Optional.ofNullable(metadata.getName()).orElse(""))
      .setNamespace(Optional.ofNullable(metadata.getNamespace()).orElse(""))
      .putAllLabels(
        metadata.getLabels() != null ? metadata.getLabels() : Collections.emptyMap())
      .putAllAnnotations(
        metadata.getAnnotations() != null ? metadata.getAnnotations() : Collections.emptyMap())
      .build();
  }

  private CronJobSpec assembleSpec(V1CronJob cronJob) {
    if (cronJob.getSpec() == null) {
      return CronJobSpec.newBuilder().build();
    }
    var spec = cronJob.getSpec();

    return CronJobSpec.newBuilder()
      .setSchedule(Optional.ofNullable(spec.getSchedule()).orElse(""))
      .setTimeZone(Optional.ofNullable(spec.getTimeZone()).orElse(""))
      .setSuspend(Optional.ofNullable(spec.getSuspend()).orElse(false))
      .setConcurrencyPolicy(Optional.ofNullable(spec.getConcurrencyPolicy()).orElse(""))
      .setSuccessfulJobsHistoryLimit(
        Optional.ofNullable(spec.getSuccessfulJobsHistoryLimit()).orElse(0))
      .setFailedJobsHistoryLimit(
        Optional.ofNullable(spec.getFailedJobsHistoryLimit()).orElse(0))
      .setJobTemplate(assembleJobTemplate(spec.getJobTemplate()))
      .build();
  }

  private CronJobJobTemplate assembleJobTemplate(V1JobTemplateSpec template) {
    if (template == null || template.getSpec() == null) {
      return CronJobJobTemplate.newBuilder().build();
    }
    return CronJobJobTemplate.newBuilder()
      .setSpec(assembleJobSpec(template.getSpec()))
      .build();
  }

  private CronJobJobSpec assembleJobSpec(V1JobSpec jobSpec) {
    if (jobSpec == null) {
      return CronJobJobSpec.newBuilder().build();
    }
    return CronJobJobSpec.newBuilder()
      .setBackoffLimit(Optional.ofNullable(jobSpec.getBackoffLimit()).orElse(0))
      .setActiveDeadlineSeconds(
        Optional.ofNullable(jobSpec.getActiveDeadlineSeconds()).orElse(0L))
      .setTemplate(assemblePodTemplate(jobSpec.getTemplate()))
      .build();
  }

  private PodTemplate assemblePodTemplate(V1PodTemplateSpec template) {
    if (template == null) {
      return PodTemplate.newBuilder().build();
    }
    var pod = new V1Pod()
      .metadata(template.getMetadata())
      .spec(template.getSpec());
    var result = podFactory.assemblePod(pod);
    return PodTemplate.newBuilder()
      .setMetadata(result.getMetadata())
      .setSpec(result.getSpec())
      .build();
  }

  private CronJobStatus assembleStatus(V1CronJob cronJob) {
    if (cronJob.getStatus() == null) {
      return CronJobStatus.newBuilder().build();
    }
    var status = cronJob.getStatus();
    long age = 0L;
    if (cronJob.getMetadata() != null &&
      cronJob.getMetadata().getCreationTimestamp() != null) {
      age =
        System.currentTimeMillis() -
          cronJob.getMetadata().getCreationTimestamp().toEpochSecond() * 1000L;
    }
    return CronJobStatus.newBuilder()
      .setActive(Optional.ofNullable(status.getActive())
        .map(List::size)
        .orElse(0))
      .setLastScheduleTime(
        Optional.ofNullable(status.getLastScheduleTime())
          .map(t -> t.toEpochSecond() * 1000)
          .orElse(0L))
      .setLastSuccessfulTime(
        Optional.ofNullable(status.getLastSuccessfulTime())
          .map(t -> t.toEpochSecond() * 1000)
          .orElse(0L))
      .setAge(age)
      .build();
  }

  private List<CronJobEvent> assembleCronJobEvents(V1CronJob cronJob) {
    try {
      var metadata = cronJob.getMetadata();
      if (metadata == null) {
        return Lists.newArrayList();
      }
      var namespace = metadata.getNamespace();
      var name = metadata.getName();
      var fieldSelector =
        "involvedObject.kind=CronJob,involvedObject.name=" + name;
      var items =
        coreApi
          .listNamespacedEvent(namespace)
          .fieldSelector(fieldSelector)
          .execute()
          .getItems();
      return items.stream()
        .map(this::assembleCronJobEvent)
        .toList();
    } catch (Exception e) {
      log.processError(e);
      return Lists.newArrayList();
    }
  }

  private CronJobEvent assembleCronJobEvent(CoreV1Event event) {
    long timestamp = 0L;
    if (event.getLastTimestamp() != null) {
      timestamp = event.getLastTimestamp().toInstant().toEpochMilli();
    } else if (event.getEventTime() != null) {
      timestamp = event.getEventTime().toInstant().toEpochMilli();
    }
    return CronJobEvent.newBuilder()
      .setType(Optional.ofNullable(event.getType()).orElse(""))
      .setReason(Optional.ofNullable(event.getReason()).orElse(""))
      .setMessage(Optional.ofNullable(event.getMessage()).orElse(""))
      .setTimestamp(timestamp)
      .setSource(
        event.getSource() != null && event.getSource().getComponent() != null
          ? event.getSource().getComponent()
          : "")
      .build();
  }
}
