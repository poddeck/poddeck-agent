package io.poddeck.agent.metric;

import io.poddeck.common.configuration.Configuration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.commons.configuration2.AbstractConfiguration;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(staticName = "create")
public class MetricConfiguration implements Configuration {
  private int intervalSeconds;

  @Override
  public void load(AbstractConfiguration file) {
    intervalSeconds = file.getInt("metric.interval_seconds");
  }
}