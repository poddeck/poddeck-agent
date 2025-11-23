package io.poddeck.agent.communication;

import io.poddeck.common.configuration.Configuration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.commons.configuration2.AbstractConfiguration;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(staticName = "create")
public class CommunicationConfiguration implements Configuration {
  private String hostname;
  private int port;
  private String cluster;

  @Override
  public void load(AbstractConfiguration file) {
    hostname = file.getString("communication.hostname");
    port = file.getInt("communication.port");
    cluster = file.getString("communication.cluster");
  }
}
