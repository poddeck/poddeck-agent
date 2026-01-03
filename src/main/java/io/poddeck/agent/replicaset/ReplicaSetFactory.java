package io.poddeck.agent.replicaset;

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
public final class ReplicaSetFactory {
  private final CoreV1Api coreApi;
  private final PodFactory podFactory;
  private final Log log;

  public ReplicaSet assembleReplicaSet(V1ReplicaSet replicaSet) {
    return ReplicaSet.newBuilder()
      .setMetadata(assembleMetadata(replicaSet))
      .setSpec(assembleSpec(replicaSet))
      .setStatus(assembleStatus(replicaSet))
      .addAllEvents(assembleReplicaSetEvents(replicaSet))
      .setRaw(Yaml.dump(replicaSet))
      .build();
  }

  private ReplicaSetMetadata assembleMetadata(V1ReplicaSet replicaSet) {
    if (replicaSet.getMetadata() == null) {
      return ReplicaSetMetadata.newBuilder().build();
    }
    var metadata = replicaSet.getMetadata();
    var ownerReferences = metadata.getOwnerReferences() != null ?
      metadata.getOwnerReferences().stream()
        .map(this::assembleOwnerReference).toList() :
      Lists.<OwnerReference>newArrayList();

    return ReplicaSetMetadata.newBuilder()
      .setName(Optional.ofNullable(metadata.getName()).orElse(""))
      .setNamespace(Optional.ofNullable(metadata.getNamespace()).orElse(""))
      .putAllLabels(metadata.getLabels() != null ?
        metadata.getLabels() : Collections.emptyMap())
      .putAllAnnotations(metadata.getAnnotations() != null ?
        metadata.getAnnotations() : Collections.emptyMap())
      .addAllOwnerReferences(ownerReferences)
      .build();
  }

  private OwnerReference assembleOwnerReference(V1OwnerReference ownerRef) {
    if (ownerRef == null) {
      return OwnerReference.newBuilder().build();
    }
    return OwnerReference.newBuilder()
      .setApiVersion(Optional.ofNullable(ownerRef.getApiVersion()).orElse(""))
      .setKind(Optional.ofNullable(ownerRef.getKind()).orElse(""))
      .setName(Optional.ofNullable(ownerRef.getName()).orElse(""))
      .setUid(Optional.ofNullable(ownerRef.getUid()).orElse(""))
      .setController(Optional.ofNullable(ownerRef.getController()).orElse(false))
      .setBlockOwnerDeletion(Optional.ofNullable(ownerRef.getBlockOwnerDeletion())
        .orElse(false))
      .build();
  }

  private ReplicaSetSpec assembleSpec(V1ReplicaSet replicaSet) {
    if (replicaSet.getSpec() == null) {
      return ReplicaSetSpec.newBuilder().build();
    }
    var spec = replicaSet.getSpec();
    return ReplicaSetSpec.newBuilder()
      .setReplicas(spec.getReplicas() != null ? spec.getReplicas() : 0)
      .setSelector(assembleSelector(spec.getSelector()))
      .setTemplate(assemblePodTemplate(spec.getTemplate()))
      .setMinReadySeconds(spec.getMinReadySeconds() != null ?
        spec.getMinReadySeconds() : 0)
      .build();
  }

  private ReplicaSetSelector assembleSelector(V1LabelSelector selector) {
    if (selector == null) {
      return ReplicaSetSelector.newBuilder().build();
    }
    return ReplicaSetSelector.newBuilder()
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

  private ReplicaSetStatus assembleStatus(V1ReplicaSet replicaSet) {
    if (replicaSet.getStatus() == null) {
      return ReplicaSetStatus.newBuilder().build();
    }
    var status = replicaSet.getStatus();
    var conditions = status.getConditions() != null ?
      status.getConditions().stream()
        .map(this::assembleReplicaSetCondition).toList() :
      Lists.<ReplicaSetCondition>newArrayList();

    var age = 0L;
    if (replicaSet.getMetadata() != null &&
      replicaSet.getMetadata().getCreationTimestamp() != null
    ) {
      age = System.currentTimeMillis() -
        replicaSet.getMetadata().getCreationTimestamp().toEpochSecond() * 1000L;
    }

    return ReplicaSetStatus.newBuilder()
      .setReplicas(Optional.ofNullable(status.getReplicas()).orElse(0))
      .setFullyLabeledReplicas(Optional.ofNullable(status.getFullyLabeledReplicas())
        .orElse(0))
      .setReadyReplicas(Optional.ofNullable(status.getReadyReplicas()).orElse(0))
      .setAvailableReplicas(Optional.ofNullable(status.getAvailableReplicas())
        .orElse(0))
      .setObservedGeneration(Optional.ofNullable(status.getObservedGeneration())
        .map(Long::intValue).orElse(0))
      .setAge(age)
      .addAllConditions(conditions)
      .build();
  }

  private ReplicaSetCondition assembleReplicaSetCondition(
    V1ReplicaSetCondition condition
  ) {
    if (condition == null) {
      return ReplicaSetCondition.newBuilder().build();
    }
    return ReplicaSetCondition.newBuilder()
      .setType(Optional.ofNullable(condition.getType()).orElse(""))
      .setStatus(Optional.ofNullable(condition.getStatus()).orElse(""))
      .setReason(Optional.ofNullable(condition.getReason()).orElse(""))
      .setMessage(Optional.ofNullable(condition.getMessage()).orElse(""))
      .setLastTransitionTime(Optional.ofNullable(condition.getLastTransitionTime())
        .orElse(OffsetDateTime.now()).toEpochSecond() * 1000)
      .build();
  }

  private List<ReplicaSetEvent> assembleReplicaSetEvents(V1ReplicaSet replicaSet) {
    try {
      var metadata = replicaSet.getMetadata();
      if (metadata == null) {
        return Lists.newArrayList();
      }
      var namespace = metadata.getNamespace();
      var name = metadata.getName();
      var fieldSelector = "involvedObject.kind=ReplicaSet,involvedObject.name=" + name;
      var items = coreApi.listNamespacedEvent(namespace)
        .fieldSelector(fieldSelector)
        .execute().getItems();
      return items.stream()
        .map(this::assembleReplicaSetEvent)
        .toList();
    } catch (Exception exception) {
      log.processError(exception);
      return Lists.newArrayList();
    }
  }

  private ReplicaSetEvent assembleReplicaSetEvent(CoreV1Event event) {
    long timestamp = 0;
    if (event.getLastTimestamp() != null) {
      timestamp = event.getLastTimestamp().toInstant().toEpochMilli();
    } else if (event.getEventTime() != null) {
      timestamp = event.getEventTime().toInstant().toEpochMilli();
    }
    return ReplicaSetEvent.newBuilder()
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