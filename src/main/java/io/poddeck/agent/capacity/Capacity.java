package io.poddeck.agent.capacity;

import io.kubernetes.client.custom.Quantity;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@RequiredArgsConstructor(staticName = "of")
public final class Capacity {
  private final Map<String, Quantity> capacity;

  public long cpu() {
    var cpu = capacity.get("cpu");
    if (cpu == null) {
      return 0;
    }
    return cpu.getNumber().multiply(BigDecimal.valueOf(1000)).longValue();
  }

  public long memory() {
    var memory = capacity.get("memory");
    if (memory == null) {
      return 0;
    }
    return memory.getNumber().longValue();
  }
}
