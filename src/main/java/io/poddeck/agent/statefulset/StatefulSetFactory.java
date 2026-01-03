package io.poddeck.agent.statefulset;

import com.google.api.client.util.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.AppsV1Api;
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
import java.util.stream.Collectors;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class StatefulSetFactory {
  private final CoreV1Api coreApi;
  private final AppsV1Api appsApi;
  private final PodFactory podFactory;
  private final Log log;

  public StatefulSet assembleStatefulSet(V1StatefulSet statefulSet) {
    return StatefulSet.newBuilder()
      .setMetadata(assembleMetadata(statefulSet))
      .setSpec(assembleSpec(statefulSet))
      .setStatus(assembleStatus(statefulSet))
      .addAllEvents(assembleStatefulSetEvents(statefulSet))
      .setRaw(Yaml.dump(statefulSet))
      .build();
  }

  private StatefulSetMetadata assembleMetadata(V1StatefulSet statefulSet) {
    if (statefulSet.getMetadata() == null) {
      return StatefulSetMetadata.newBuilder().build();
    }
    var metadata = statefulSet.getMetadata();
    return StatefulSetMetadata.newBuilder()
      .setName(Optional.ofNullable(metadata.getName()).orElse(""))
      .setNamespace(Optional.ofNullable(metadata.getNamespace()).orElse(""))
      .putAllLabels(metadata.getLabels() != null ?
        metadata.getLabels() : Collections.emptyMap())
      .putAllAnnotations(metadata.getAnnotations() != null ?
        metadata.getAnnotations() : Collections.emptyMap())
      .build();
  }

  private StatefulSetSpec assembleSpec(V1StatefulSet statefulSet) {
    if (statefulSet.getSpec() == null) {
      return StatefulSetSpec.newBuilder().build();
    }
    var spec = statefulSet.getSpec();
    var volumeClaimTemplates = spec.getVolumeClaimTemplates() != null ?
      spec.getVolumeClaimTemplates().stream()
        .map(this::assemblePersistentVolumeClaim).toList() :
      Lists.<PersistentVolumeClaim>newArrayList();

    return StatefulSetSpec.newBuilder()
      .setReplicas(spec.getReplicas() != null ? spec.getReplicas() : 0)
      .setSelector(assembleSelector(spec.getSelector()))
      .setTemplate(assemblePodTemplate(spec.getTemplate()))
      .setServiceName(Optional.ofNullable(spec.getServiceName()).orElse(""))
      .setPodManagementPolicy(Optional.ofNullable(spec.getPodManagementPolicy())
        .orElse(""))
      .setUpdateStrategy(assembleUpdateStrategy(spec.getUpdateStrategy()))
      .setRevisionHistoryLimit(spec.getRevisionHistoryLimit() != null ?
        spec.getRevisionHistoryLimit() : 0)
      .addAllVolumeClaimTemplates(volumeClaimTemplates)
      .build();
  }

  private StatefulSetSelector assembleSelector(V1LabelSelector selector) {
    if (selector == null) {
      return StatefulSetSelector.newBuilder().build();
    }
    return StatefulSetSelector.newBuilder()
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

  private StatefulSetUpdateStrategy assembleUpdateStrategy(
    V1StatefulSetUpdateStrategy strategy
  ) {
    if (strategy == null) {
      return StatefulSetUpdateStrategy.newBuilder().build();
    }
    return StatefulSetUpdateStrategy.newBuilder()
      .setType(Optional.ofNullable(strategy.getType()).orElse(""))
      .setRollingUpdate(assembleRollingUpdateStrategy(strategy.getRollingUpdate()))
      .build();
  }

  private RollingUpdateStatefulSetStrategy assembleRollingUpdateStrategy(
    V1RollingUpdateStatefulSetStrategy rollingUpdate
  ) {
    if (rollingUpdate == null) {
      return RollingUpdateStatefulSetStrategy.newBuilder().build();
    }

    int partition = 0;
    if (rollingUpdate.getPartition() != null) {
      partition = rollingUpdate.getPartition();
    }

    int maxUnavailable = 0;
    if (rollingUpdate.getMaxUnavailable() != null) {
      if (rollingUpdate.getMaxUnavailable().getIntValue() != null) {
        maxUnavailable = rollingUpdate.getMaxUnavailable().getIntValue();
      }
    }

    return RollingUpdateStatefulSetStrategy.newBuilder()
      .setPartition(partition)
      .setMaxUnavailable(maxUnavailable)
      .build();
  }

  private PersistentVolumeClaim assemblePersistentVolumeClaim(
    V1PersistentVolumeClaim pvc
  ) {
    if (pvc == null) {
      return PersistentVolumeClaim.newBuilder().build();
    }
    var name = "";
    if (pvc.getMetadata() != null && pvc.getMetadata().getName() != null) {
      name = pvc.getMetadata().getName();
    }
    return PersistentVolumeClaim.newBuilder()
      .setName(name)
      .setSpec(assemblePersistentVolumeClaimSpec(pvc.getSpec()))
      .build();
  }

  private PersistentVolumeClaimSpec assemblePersistentVolumeClaimSpec(
    V1PersistentVolumeClaimSpec spec
  ) {
    if (spec == null) {
      return PersistentVolumeClaimSpec.newBuilder().build();
    }
    var accessModes = spec.getAccessModes() != null ?
      spec.getAccessModes() : Lists.<String>newArrayList();

    return PersistentVolumeClaimSpec.newBuilder()
      .addAllAccessModes(accessModes)
      .setResources(assemblePersistentVolumeResources(spec.getResources()))
      .setStorageClassName(Optional.ofNullable(spec.getStorageClassName())
        .orElse(""))
      .setVolumeMode(Optional.ofNullable(spec.getVolumeMode()).orElse(""))
      .build();
  }

  private PersistentVolumeResources assemblePersistentVolumeResources(
    V1VolumeResourceRequirements resources
  ) {
    if (resources == null) {
      return PersistentVolumeResources.newBuilder().build();
    }

    var requests = resources.getRequests() != null ?
      resources.getRequests().entrySet().stream()
        .collect(Collectors.toMap(
          e -> e.getKey(),
          e -> e.getValue().toSuffixedString()
        )) : Collections.<String, String>emptyMap();

    var limits = resources.getLimits() != null ?
      resources.getLimits().entrySet().stream()
        .collect(Collectors.toMap(
          e -> e.getKey(),
          e -> e.getValue().toSuffixedString()
        )) : Collections.<String, String>emptyMap();

    return PersistentVolumeResources.newBuilder()
      .putAllRequests(requests)
      .putAllLimits(limits)
      .build();
  }

  private StatefulSetStatus assembleStatus(V1StatefulSet statefulSet) {
    if (statefulSet.getStatus() == null) {
      return StatefulSetStatus.newBuilder().build();
    }
    var status = statefulSet.getStatus();
    var conditions = status.getConditions() != null ?
      status.getConditions().stream()
        .map(this::assembleStatefulSetCondition).toList() :
      Lists.<StatefulSetCondition>newArrayList();

    var age = 0L;
    if (statefulSet.getMetadata() != null &&
      statefulSet.getMetadata().getCreationTimestamp() != null
    ) {
      age = System.currentTimeMillis() -
        statefulSet.getMetadata().getCreationTimestamp().toEpochSecond() * 1000L;
    }

    return StatefulSetStatus.newBuilder()
      .setReplicas(Optional.ofNullable(status.getReplicas()).orElse(0))
      .setReadyReplicas(Optional.ofNullable(status.getReadyReplicas()).orElse(0))
      .setCurrentReplicas(Optional.ofNullable(status.getCurrentReplicas()).orElse(0))
      .setUpdatedReplicas(Optional.ofNullable(status.getUpdatedReplicas()).orElse(0))
      .setAvailableReplicas(Optional.ofNullable(status.getAvailableReplicas())
        .orElse(0))
      .setCurrentRevision(Optional.ofNullable(status.getCurrentRevision()).orElse(""))
      .setUpdateRevision(Optional.ofNullable(status.getUpdateRevision()).orElse(""))
      .setAge(age)
      .addAllConditions(conditions)
      .setCollisionCount(Optional.ofNullable(status.getCollisionCount()).orElse(0))
      .setObservedGeneration(Optional.ofNullable(status.getObservedGeneration())
        .orElse(0L))
      .build();
  }

  private StatefulSetCondition assembleStatefulSetCondition(
    V1StatefulSetCondition condition
  ) {
    if (condition == null) {
      return StatefulSetCondition.newBuilder().build();
    }
    return StatefulSetCondition.newBuilder()
      .setType(Optional.ofNullable(condition.getType()).orElse(""))
      .setStatus(Optional.ofNullable(condition.getStatus()).orElse(""))
      .setReason(Optional.ofNullable(condition.getReason()).orElse(""))
      .setMessage(Optional.ofNullable(condition.getMessage()).orElse(""))
      .setLastTransitionTime(Optional.ofNullable(condition.getLastTransitionTime())
        .orElse(OffsetDateTime.now()).toEpochSecond() * 1000)
      .build();
  }

  private List<StatefulSetEvent> assembleStatefulSetEvents(V1StatefulSet statefulSet) {
    try {
      var metadata = statefulSet.getMetadata();
      if (metadata == null) {
        return Lists.newArrayList();
      }
      var namespace = metadata.getNamespace();
      var name = metadata.getName();
      var fieldSelector = "involvedObject.kind=StatefulSet,involvedObject.name=" + name;
      var items = coreApi.listNamespacedEvent(namespace)
        .fieldSelector(fieldSelector)
        .execute().getItems();
      return items.stream()
        .map(this::assembleStatefulSetEvent)
        .toList();
    } catch (Exception exception) {
      log.processError(exception);
      return Lists.newArrayList();
    }
  }

  private StatefulSetEvent assembleStatefulSetEvent(CoreV1Event event) {
    long timestamp = 0;
    if (event.getLastTimestamp() != null) {
      timestamp = event.getLastTimestamp().toInstant().toEpochMilli();
    } else if (event.getEventTime() != null) {
      timestamp = event.getEventTime().toInstant().toEpochMilli();
    }
    return StatefulSetEvent.newBuilder()
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