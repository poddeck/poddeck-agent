package io.poddeck.agent.event;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.CoreV1Event;
import io.kubernetes.client.util.Watch;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.common.Event;
import io.poddeck.common.EventReport;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class EventWatcher {
  private final ApiClient apiClient;
  private final CoreV1Api coreApi;
  private final CommunicationClient communicationClient;
  private final Log log;

  /*public void watch() {
    System.out.println("WATCH");
    String resourceVersion = null;
    var backoff = 1000L;
    while (!Thread.currentThread().isInterrupted()) {
      try {
        resourceVersion = ensureResourceVersion(resourceVersion);
        resourceVersion = startWatchLoop(resourceVersion);
        backoff = applyBackoff(backoff);
      } catch (Exception exception) {
        log.processError(exception);
        resourceVersion = null;
      }
    }
  }*/

  public void watch() {
    String resourceVersion = null;
    long baseBackoffMs = 1000;
    long maxBackoffMs = 30_000;
    while (!Thread.currentThread().isInterrupted()) {
      try {
        if (resourceVersion == null) {
          var list = coreApi.listEventForAllNamespaces().execute();
          if (list != null && list.getMetadata() != null) {
            resourceVersion = list.getMetadata().getResourceVersion();
          }
        }
        var call = coreApi.listEventForAllNamespaces().watch(true)
          .resourceVersion(resourceVersion).timeoutSeconds(60).buildCall(null);
        try (Watch<CoreV1Event> watch = Watch.createWatch(
          apiClient,
          call,
          new TypeToken<Watch.Response<CoreV1Event>>() {
          }.getType())) {
          for (Watch.Response<CoreV1Event> event : watch) {
            processEvent(event.object);
            if (event.object != null && event.object.getMetadata() != null) {
              resourceVersion = event.object.getMetadata().getResourceVersion();
            }
          }
        }
        Thread.sleep(Math.min(maxBackoffMs, baseBackoffMs));
        baseBackoffMs = Math.min(maxBackoffMs, baseBackoffMs * 2);
      } catch (ApiException exception) {
        if (exception.getCode() == 410) {
          resourceVersion = null;
        }
      } catch (Exception exception) {
        exception.printStackTrace();
      }
    }
  }

  private String ensureResourceVersion(String resourceVersion) throws Exception {
    if (resourceVersion != null) {
      return resourceVersion;
    }
    var list = coreApi.listEventForAllNamespaces().execute();
    if (list != null && list.getMetadata() != null) {
      return list.getMetadata().getResourceVersion();
    }
    return null;
  }

  private String startWatchLoop(String resourceVersion) throws Exception {
    var call = coreApi.listEventForAllNamespaces().watch(true)
      .resourceVersion(resourceVersion).timeoutSeconds(60).buildCall(null);
    var token = new TypeToken<Watch.Response<CoreV1Event>>() {}.getType();
    try (Watch<CoreV1Event> watch = Watch.createWatch(apiClient, call, token)) {
      for (var event : watch) {
        processEvent(event.object);
        if (event.object != null) {
          resourceVersion = event.object.getMetadata().getResourceVersion();
        }
      }
    }
    return resourceVersion;
  }


  private void processEvent(CoreV1Event event) {
    var result = Event.newBuilder()
      .setName(event.getMetadata().getName() != null ?
        event.getMetadata().getName() : "")
      .setNamespace(event.getMetadata().getNamespace() != null ?
        event.getMetadata().getNamespace() : "")
      .setInvolvedObjectKind(event.getInvolvedObject().getKind() != null ?
        event.getInvolvedObject().getKind() : "")
      .setInvolvedObjectName(event.getInvolvedObject().getName() != null ?
        event.getInvolvedObject().getName() : "")
      .setReason(event.getReason() != null ? event.getReason() : "")
      .setMessage(event.getMessage() != null ? event.getMessage() : "")
      .setType(event.getType() != null ? event.getType() : "")
      .setCount(event.getCount() != null ? event.getCount() : 0)
      .setFirstTimestamp(event.getFirstTimestamp() != null ?
        event.getFirstTimestamp().toInstant().toEpochMilli() : -1)
      .setLastTimestamp(event.getLastTimestamp() != null ?
        event.getLastTimestamp().toInstant().toEpochMilli() : -1)
      .build();
    communicationClient.send(EventReport.newBuilder()
      .setEvent(result).build());
  }

  private static final long MAX_BACKOFF = 30_000L;

  private long applyBackoff(long backoff) throws Exception {
    Thread.sleep(Math.min(MAX_BACKOFF, backoff));
    return Math.min(MAX_BACKOFF, backoff * 2);
  }
}
