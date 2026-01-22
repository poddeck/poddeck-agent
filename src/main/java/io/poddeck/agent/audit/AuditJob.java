package io.poddeck.agent.audit;

import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.util.Yaml;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

@Accessors(fluent = true)
@RequiredArgsConstructor(staticName = "create")
public final class AuditJob {
  public static AuditJob createAndLoad() throws Exception {
    var job = create();
    job.load();
    return job;
  }

  @Getter
  private V1Job job;

  private static final String CONFIGURATION_PATH = "audit/job.yaml";

  public void load() throws Exception {
    var loader = getClass().getClassLoader();
    try (var inputStream = loader.getResourceAsStream(CONFIGURATION_PATH)) {
      job = Yaml.loadAs(readFromInputStream(inputStream), V1Job.class);
    }
  }

  private String readFromInputStream(InputStream inputStream) throws IOException {
    var result = new StringBuilder();
    try (var reader = new BufferedReader(new InputStreamReader(inputStream))) {
      String line;
      while ((line = reader.readLine()) != null) {
        result.append(line).append("\n");
      }
    }
    return result.toString();
  }
}