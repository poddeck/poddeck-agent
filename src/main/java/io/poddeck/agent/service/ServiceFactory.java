package io.poddeck.agent.service;

import com.google.api.client.util.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.CoreV1Event;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServicePort;
import io.kubernetes.client.util.Yaml;
import io.poddeck.common.*;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class ServiceFactory {
  private final CoreV1Api coreApi;
  private final Log log;

  public Service assembleService(V1Service service) {
    return Service.newBuilder()
      .setMetadata(assembleMetadata(service))
      .setSpec(assembleSpec(service))
      .setStatus(assembleStatus(service))
      .addAllEvents(assembleServiceEvents(service))
      .setRaw(Yaml.dump(service))
      .build();
  }

  private ServiceMetadata assembleMetadata(V1Service service) {
    if (service.getMetadata() == null) {
      return ServiceMetadata.newBuilder().build();
    }
    var metadata = service.getMetadata();
    return ServiceMetadata.newBuilder()
      .setName(Optional.ofNullable(metadata.getName()).orElse(""))
      .setNamespace(Optional.ofNullable(metadata.getNamespace()).orElse(""))
      .putAllLabels(metadata.getLabels() != null
        ? metadata.getLabels()
        : Collections.emptyMap())
      .putAllAnnotations(metadata.getAnnotations() != null
        ? metadata.getAnnotations()
        : Collections.emptyMap())
      .build();
  }

  private ServiceSpec assembleSpec(V1Service service) {
    if (service.getSpec() == null) {
      return ServiceSpec.newBuilder().build();
    }
    var spec = service.getSpec();

    return ServiceSpec.newBuilder()
      .setType(Optional.ofNullable(spec.getType()).orElse(""))
      .putAllSelector(spec.getSelector() != null
        ? spec.getSelector()
        : Collections.emptyMap())
      .addAllPorts(assemblePorts(spec.getPorts()))
      .setClusterIp(Optional.ofNullable(spec.getClusterIP()).orElse(""))
      .addAllClusterIps(spec.getClusterIPs() != null
        ? spec.getClusterIPs()
        : Lists.newArrayList())
      .setIpFamilyPolicy(Optional.ofNullable(spec.getIpFamilyPolicy()).orElse(""))
      .addAllIpFamilies(spec.getIpFamilies() != null
        ? spec.getIpFamilies()
        : Lists.newArrayList())
      .setSessionAffinity(Optional.ofNullable(spec.getSessionAffinity()).orElse(""))
      .setInternalTrafficPolicy(
        Optional.ofNullable(spec.getInternalTrafficPolicy()).orElse(""))
      .setExternalTrafficPolicy(
        Optional.ofNullable(spec.getExternalTrafficPolicy()).orElse(""))
      .build();
  }

  private List<ServicePort> assemblePorts(List<V1ServicePort> ports) {
    if (ports == null) {
      return Lists.newArrayList();
    }
    return ports.stream()
      .map(this::assemblePort)
      .toList();
  }

  private ServicePort assemblePort(V1ServicePort port) {
    if (port == null) {
      return ServicePort.newBuilder().build();
    }
    return ServicePort.newBuilder()
      .setName(Optional.ofNullable(port.getName()).orElse(""))
      .setPort(Optional.ofNullable(port.getPort()).orElse(0))
      .setProtocol(Optional.ofNullable(port.getProtocol()).orElse(""))
      .setTargetPort(
        port.getTargetPort() != null ? port.getTargetPort().toString() : "")
      .setNodePort(Optional.ofNullable(port.getNodePort()).orElse(0))
      .build();
  }

  private ServiceStatus assembleStatus(V1Service service) {
    var builder = ServiceStatus.newBuilder();

    if (service.getMetadata() != null &&
      service.getMetadata().getCreationTimestamp() != null
    ) {
      var age = System.currentTimeMillis() -
        service.getMetadata().getCreationTimestamp().toEpochSecond() * 1000L;
      builder.setAge(age);
    }

    if (service.getStatus() != null &&
      service.getStatus().getLoadBalancer() != null &&
      service.getStatus().getLoadBalancer().getIngress() != null
    ) {
      service.getStatus().getLoadBalancer().getIngress()
        .forEach(ingress -> builder.addLoadBalancer(
          LoadBalancerIngress.newBuilder()
            .setIp(Optional.ofNullable(ingress.getIp()).orElse(""))
            .setHostname(Optional.ofNullable(ingress.getHostname()).orElse(""))
            .build()
        ));
    }

    builder.addAllEndpoints(assembleEndpoints(service));

    return builder.build();
  }

  private List<ServiceEndpoint> assembleEndpoints(V1Service service) {
    try {
      var metadata = service.getMetadata();
      if (metadata == null) {
        return Lists.newArrayList();
      }

      var endpoints = coreApi
        .readNamespacedEndpoints(metadata.getName(), metadata.getNamespace())
        .execute();

      if (endpoints.getSubsets() == null) {
        return Lists.newArrayList();
      }

      var result = Lists.<ServiceEndpoint>newArrayList();

      for (var subset : endpoints.getSubsets()) {
        if (subset.getAddresses() == null || subset.getPorts() == null) {
          continue;
        }
        for (var addr : subset.getAddresses()) {
          for (var port : subset.getPorts()) {
            result.add(ServiceEndpoint.newBuilder()
              .setIp(Optional.ofNullable(addr.getIp()).orElse(""))
              .setPort(Optional.ofNullable(port.getPort()).orElse(0))
              .build());
          }
        }
      }
      return result;
    } catch (Exception e) {
      log.processError(e);
      return Lists.newArrayList();
    }
  }

  private List<ServiceEvent> assembleServiceEvents(V1Service service) {
    try {
      var metadata = service.getMetadata();
      if (metadata == null) {
        return Lists.newArrayList();
      }
      var namespace = metadata.getNamespace();
      var name = metadata.getName();
      var fieldSelector =
        "involvedObject.kind=Service,involvedObject.name=" + name;

      var items = coreApi.listNamespacedEvent(namespace)
        .fieldSelector(fieldSelector)
        .execute()
        .getItems();

      return items.stream()
        .map(this::assembleServiceEvent)
        .toList();
    } catch (Exception e) {
      log.processError(e);
      return Lists.newArrayList();
    }
  }

  private ServiceEvent assembleServiceEvent(CoreV1Event event) {
    long timestamp = 0;
    if (event.getLastTimestamp() != null) {
      timestamp = event.getLastTimestamp().toInstant().toEpochMilli();
    } else if (event.getEventTime() != null) {
      timestamp = event.getEventTime().toInstant().toEpochMilli();
    }

    return ServiceEvent.newBuilder()
      .setType(Optional.ofNullable(event.getType()).orElse(""))
      .setReason(Optional.ofNullable(event.getReason()).orElse(""))
      .setMessage(Optional.ofNullable(event.getMessage()).orElse(""))
      .setTimestamp(timestamp)
      .setSource(event.getSource() != null &&
        event.getSource().getComponent() != null
        ? event.getSource().getComponent()
        : "")
      .build();
  }
}
