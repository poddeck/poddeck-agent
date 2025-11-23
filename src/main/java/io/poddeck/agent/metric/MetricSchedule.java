package io.poddeck.agent.metric;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.poddeck.agent.communication.CommunicationClient;
import io.poddeck.agent.telegraf.TelegrafConfiguration;
import io.poddeck.agent.telegraf.TelegrafMetricBody;
import io.poddeck.agent.telegraf.TelegrafRequestFactory;
import io.poddeck.common.Metric;
import io.poddeck.common.MetricReport;
import io.poddeck.common.iterator.AsyncIterator;
import io.poddeck.common.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class MetricSchedule {
  private final Log log;
  private final CoreV1Api coreApi;
  private final TelegrafConfiguration telegrafConfiguration;
  private final TelegrafRequestFactory telegrafRequestFactory;
  private final MetricConfiguration metricConfiguration;
  private final CommunicationClient client;
  private final ScheduledExecutorService executorService =
    Executors.newScheduledThreadPool(10);
  private ScheduledFuture<?> scheduler;

  private static final TimeUnit TIME_UNIT = TimeUnit.MILLISECONDS;

  public void start() {
    scheduler = executorService.scheduleAtFixedRate(this::execute, 0,
      metricConfiguration.intervalSeconds() * 1000L, TIME_UNIT);
  }

  private void execute() {
    try {
      var telegrafPods = coreApi
        .listNamespacedPod(telegrafConfiguration.namespace()).execute();
      AsyncIterator.execute(telegrafPods.getItems(),
          pod -> telegrafRequestFactory
          .create(pod.getStatus().getPodIP(), telegrafConfiguration.port())
          .send("/metrics", "GET")
          .thenApply(response -> createMetric(pod.getSpec().getNodeName(),
            response.body())))
        .thenAccept(metrics -> client.send(MetricReport.newBuilder()
          .addAllMetrics(metrics).build()));
    } catch (Exception exception) {
      log.processError(exception);
    }
  }

  private Metric createMetric(String node, String raw) {
    var body = TelegrafMetricBody.create(raw);
    var cpuCores = body.countMetricLines("cpu_usage_idle") - 1;
    var cpuRatio = 100 - body.extractMetric("cpu_usage_idle", "cpu", "cpu-total");
    var totalMemory = body.extractMetric("mem_total");
    var usedMemory = body.extractMetric("mem_used");
    var memoryRatio = body.extractMetric("mem_used_percent");
    var totalDiskSpace = body.extractMetric("disk_total", "path", "/");
    var usedDiskSpace = body.extractMetric("disk_used", "path", "/");
    var diskSpaceRatio = body.extractMetric("disk_used_percent");
    return Metric.newBuilder()
      .setNode(node)
      .setCpuCores(cpuCores)
      .setCpuRatio(cpuRatio)
      .setTotalMemory(byteToGigaByte(totalMemory))
      .setUsedMemory(byteToGigaByte(usedMemory))
      .setMemoryRatio(memoryRatio)
      .setTotalStorage(byteToGigaByte(totalDiskSpace))
      .setUsedStorage(byteToGigaByte(usedDiskSpace))
      .setStorageRatio(diskSpaceRatio)
      .build();
  }

  private double byteToGigaByte(double value) {
    return value / 1024 / 1024 / 1024;
  }

  public void stop() {
    scheduler.cancel(false);
  }
}
