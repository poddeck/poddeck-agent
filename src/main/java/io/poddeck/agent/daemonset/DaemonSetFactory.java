package io.poddeck.agent.daemonset;

import com.google.api.client.util.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
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
public final class DaemonSetFactory {
  private final CoreV1Api coreApi;
  private final PodFactory podFactory;
  private final Log log;

  public DaemonSet assembleDaemonSet(V1DaemonSet daemonSet) {
    return DaemonSet.newBuilder()
      .setMetadata(assembleMetadata(daemonSet))
      .setSpec(assembleSpec(daemonSet))
      .setStatus(assembleStatus(daemonSet))
      .addAllEvents(assembleDaemonSetEvents(daemonSet))
      .setRaw(Yaml.dump(daemonSet))
      .build();
  }

  private DaemonSetMetadata assembleMetadata(V1DaemonSet daemonSet) {
    if (daemonSet.getMetadata() == null) {
      return DaemonSetMetadata.newBuilder().build();
    }
    var metadata = daemonSet.getMetadata();
    return DaemonSetMetadata.newBuilder()
      .setName(Optional.ofNullable(metadata.getName()).orElse(""))
      .setNamespace(Optional.ofNullable(metadata.getNamespace()).orElse(""))
      .putAllLabels(metadata.getLabels() != null ?
        metadata.getLabels() : Collections.emptyMap())
      .putAllAnnotations(metadata.getAnnotations() != null ?
        metadata.getAnnotations() : Collections.emptyMap())
      .build();
  }

  private DaemonSetSpec assembleSpec(V1DaemonSet daemonSet) {
    if (daemonSet.getSpec() == null) {
      return DaemonSetSpec.newBuilder().build();
    }
    var spec = daemonSet.getSpec();
    return DaemonSetSpec.newBuilder()
      .setSelector(assembleSelector(spec.getSelector()))
      .setTemplate(assemblePodTemplate(spec.getTemplate()))
      .setUpdateStrategy(assembleUpdateStrategy(spec.getUpdateStrategy()))
      .setMinReadySeconds(spec.getMinReadySeconds() != null ?
        spec.getMinReadySeconds() : 0)
      .setRevisionHistoryLimit(spec.getRevisionHistoryLimit() != null ?
        spec.getRevisionHistoryLimit() : 0)
      .build();
  }

  private DaemonSetSelector assembleSelector(V1LabelSelector selector) {
    if (selector == null) {
      return DaemonSetSelector.newBuilder().build();
    }
    return DaemonSetSelector.newBuilder()
      .putAllMatchLabels(selector.getMatchLabels() != null ?
        selector.getMatchLabels() : Collections.emptyMap())
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

  private DaemonSetUpdateStrategy assembleUpdateStrategy(
    V1DaemonSetUpdateStrategy strategy
  ) {
    if (strategy == null) {
      return DaemonSetUpdateStrategy.newBuilder().build();
    }
    return DaemonSetUpdateStrategy.newBuilder()
      .setType(Optional.ofNullable(strategy.getType()).orElse(""))
      .setRollingUpdate(assembleRollingUpdate(strategy.getRollingUpdate()))
      .build();
  }

  private DaemonSetRollingUpdate assembleRollingUpdate(
    V1RollingUpdateDaemonSet rollingUpdate
  ) {
    if (rollingUpdate == null) {
      return DaemonSetRollingUpdate.newBuilder().build();
    }

    int maxUnavailable = 0;
    if (rollingUpdate.getMaxUnavailable() != null) {
      if (rollingUpdate.getMaxUnavailable().getIntValue() != null) {
        maxUnavailable = rollingUpdate.getMaxUnavailable().getIntValue();
      }
    }

    int maxSurge = 0;
    if (rollingUpdate.getMaxSurge() != null) {
      if (rollingUpdate.getMaxSurge().getIntValue() != null) {
        maxSurge = rollingUpdate.getMaxSurge().getIntValue();
      }
    }

    return DaemonSetRollingUpdate.newBuilder()
      .setMaxUnavailable(maxUnavailable)
      .setMaxSurge(maxSurge)
      .build();
  }

  private DaemonSetStatus assembleStatus(V1DaemonSet daemonSet) {
    if (daemonSet.getStatus() == null) {
      return DaemonSetStatus.newBuilder().build();
    }
    var status = daemonSet.getStatus();
    var conditions = status.getConditions() != null ?
      status.getConditions().stream()
        .map(this::assembleDaemonSetCondition).toList() :
      Lists.<DaemonSetCondition>newArrayList();

    var age = 0L;
    if (daemonSet.getMetadata() != null &&
      daemonSet.getMetadata().getCreationTimestamp() != null
    ) {
      age = System.currentTimeMillis() -
        daemonSet.getMetadata().getCreationTimestamp().toEpochSecond() * 1000L;
    }

    return DaemonSetStatus.newBuilder()
      .setCurrentNumberScheduled(Optional.ofNullable(status.getCurrentNumberScheduled())
        .orElse(0))
      .setNumberMisscheduled(Optional.ofNullable(status.getNumberMisscheduled())
        .orElse(0))
      .setDesiredNumberScheduled(Optional.ofNullable(status.getDesiredNumberScheduled())
        .orElse(0))
      .setNumberReady(Optional.ofNullable(status.getNumberReady()).orElse(0))
      .setUpdatedNumberScheduled(Optional.ofNullable(status.getUpdatedNumberScheduled())
        .orElse(0))
      .setNumberAvailable(Optional.ofNullable(status.getNumberAvailable())
        .orElse(0))
      .setNumberUnavailable(Optional.ofNullable(status.getNumberUnavailable())
        .orElse(0))
      .setAge(age)
      .setUpdatedRevision(Optional.ofNullable(status.getUpdatedNumberScheduled())
        .map(Object::toString).orElse(""))
      .setCurrentRevision(Optional.ofNullable(status.getObservedGeneration())
        .map(Object::toString).orElse(""))
      .addAllConditions(conditions)
      .build();
  }

  private DaemonSetCondition assembleDaemonSetCondition(
    V1DaemonSetCondition condition
  ) {
    if (condition == null) {
      return DaemonSetCondition.newBuilder().build();
    }
    return DaemonSetCondition.newBuilder()
      .setType(Optional.ofNullable(condition.getType()).orElse(""))
      .setStatus(Optional.ofNullable(condition.getStatus()).orElse(""))
      .setReason(Optional.ofNullable(condition.getReason()).orElse(""))
      .setMessage(Optional.ofNullable(condition.getMessage()).orElse(""))
      .setLastUpdate(Optional.ofNullable(condition.getLastTransitionTime())
        .orElse(OffsetDateTime.now()).toEpochSecond() * 1000)
      .build();
  }

  private List<DaemonSetEvent> assembleDaemonSetEvents(V1DaemonSet daemonSet) {
    try {
      var metadata = daemonSet.getMetadata();
      if (metadata == null) {
        return Lists.newArrayList();
      }
      var namespace = metadata.getNamespace();
      var name = metadata.getName();
      var fieldSelector = "involvedObject.kind=DaemonSet,involvedObject.name=" + name;
      var items = coreApi.listNamespacedEvent(namespace)
        .fieldSelector(fieldSelector)
        .execute().getItems();
      return items.stream()
        .map(this::assembleDaemonSetEvent)
        .toList();
    } catch (Exception exception) {
      log.processError(exception);
      return Lists.newArrayList();
    }
  }

  private DaemonSetEvent assembleDaemonSetEvent(CoreV1Event event) {
    long timestamp = 0;
    if (event.getLastTimestamp() != null) {
      timestamp = event.getLastTimestamp().toInstant().toEpochMilli();
    } else if (event.getEventTime() != null) {
      timestamp = event.getEventTime().toInstant().toEpochMilli();
    }
    return DaemonSetEvent.newBuilder()
      .setType(Optional.ofNullable(event.getType()).orElse(""))
      .setReason(Optional.ofNullable(event.getReason()).orElse(""))
      .setMessage(Optional.ofNullable(event.getMessage()).orElse(""))
      .setTimestamp(timestamp)
      .setSource(event.getSource() != null &&
        event.getSource().getComponent() != null
        ? event.getSource().getComponent() : "")
      .build();
  }
}