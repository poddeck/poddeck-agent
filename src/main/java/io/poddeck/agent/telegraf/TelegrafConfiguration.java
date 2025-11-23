package io.poddeck.agent.telegraf;

import io.poddeck.common.configuration.Configuration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.commons.configuration2.AbstractConfiguration;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(staticName = "create")
public class TelegrafConfiguration implements Configuration {
  private String namespace;
  private int port;

  @Override
  public void load(AbstractConfiguration file) {
    namespace = file.getString("telegraf.namespace");
    port = file.getInt("telegraf.port");
  }
}