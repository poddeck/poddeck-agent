package io.poddeck.agent.telegraf;

import lombok.RequiredArgsConstructor;

import java.util.function.Function;

@RequiredArgsConstructor(staticName = "create")
public class TelegrafMetricBody {
  private final String raw;

  public int countMetricLines(String metricName) {
    return countMetricLines(line -> line.startsWith(metricName));
  }

  public int countMetricLines(Function<String, Boolean> finder) {
    int count = 0;
    for (var line : raw.split("\n")) {
      if (finder.apply(line)) {
        count++;
      }
    }
    return count;
  }

  public double extractMetric(
    String metricName, String secondaryFilterKey, String secondaryFilterValue
  ) {
    return extractMetric(line -> line.startsWith(metricName) &&
      line.contains(secondaryFilterKey + "=\"" + secondaryFilterValue + "\""));
  }

  public double extractMetric(String metricName) {
    return extractMetric(line -> line.startsWith(metricName));
  }

  public double extractMetric(Function<String, Boolean> finder) {
    for (var line : raw.split("\n")) {
      if (finder.apply(line)) {
        var parts = line.split(" ");
        if (parts.length == 2) {
          return Double.parseDouble(parts[1]);
        }
      }
    }
    return -1;
  }
}
