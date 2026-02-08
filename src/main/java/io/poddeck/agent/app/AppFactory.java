package io.poddeck.agent.app;

import com.google.api.client.util.Lists;
import com.marcnuri.helm.Release;
import com.marcnuri.helm.SearchResult;
import io.poddeck.common.App;
import io.poddeck.common.AppVersion;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AppFactory {
  public List<App> assembleApps(List<SearchResult> results, List<Release> releases) {
    return results.stream().collect(Collectors.groupingBy(SearchResult::getName))
      .values().stream().map(group -> assembleApp(group, releases)).toList();
  }

  private App assembleApp(List<SearchResult> group, List<Release> releases) {
    var result = group.getFirst();
    var nameSplit = result.getName().split("/");
    var versions = Lists.<AppVersion>newArrayList();
    for (var entry : group) {
      versions.add(AppVersion.newBuilder()
        .setChartVersion(entry.getChartVersion())
        .setAppVersion(entry.getAppVersion())
        .build());
    }
    var installed = releases.stream()
      .anyMatch(release -> release.getChart().equals(result.getName()));
    return App.newBuilder()
      .setRepository(nameSplit.length == 2 ? nameSplit[0] : "")
      .setName(nameSplit.length == 2 ? nameSplit[1] : result.getName())
      .addAllVersions(versions)
      .setDescription(result.getDescription())
      .addAllKeywords(Arrays.stream(result.getKeywords().split(",")).toList())
      .setInstalled(installed)
      .build();
  }
}
