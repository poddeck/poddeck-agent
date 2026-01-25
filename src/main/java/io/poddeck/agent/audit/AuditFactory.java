package io.poddeck.agent.audit;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.poddeck.common.Audit;
import io.poddeck.common.AuditControl;
import io.poddeck.common.AuditResult;
import io.poddeck.common.AuditTest;
import io.poddeck.common.AuditTotals;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class AuditFactory {
  public Audit fromJson(String json) {
    var jsonObj = JsonParser.parseString(json).getAsJsonObject();
    var auditBuilder = Audit.newBuilder();
    auditBuilder.setRaw(json);
    if (jsonObj.has("Totals")) {
      auditBuilder.setTotals(parseTotals(jsonObj.getAsJsonObject("Totals")));
    }
    if (jsonObj.has("Controls")) {
      JsonArray controlsArray = jsonObj.getAsJsonArray("Controls");
      for (JsonElement elem : controlsArray) {
        auditBuilder.addControls(parseControl(elem.getAsJsonObject()));
      }
    }
    return auditBuilder.build();
  }

  private AuditControl parseControl(JsonObject obj) {
    var builder = AuditControl.newBuilder();
    Optional.ofNullable(obj.get("id"))
      .ifPresent(e -> builder.setId(e.getAsString()));
    Optional.ofNullable(obj.get("version"))
      .ifPresent(e -> builder.setVersion(e.getAsString()));
    Optional.ofNullable(obj.get("detected_version"))
      .ifPresent(e -> builder.setDetectedVersion(e.getAsString()));
    Optional.ofNullable(obj.get("text"))
      .ifPresent(e -> builder.setText(e.getAsString()));
    Optional.ofNullable(obj.get("node_type"))
      .ifPresent(e -> builder.setNodeType(e.getAsString()));
    if (obj.has("totals")) {
      builder.setTotals(parseTotals(obj.getAsJsonObject("totals")));
    }
    if (obj.has("tests")) {
      JsonArray testsArray = obj.getAsJsonArray("tests");
      for (JsonElement elem : testsArray) {
        builder.addTests(parseTest(elem.getAsJsonObject()));
      }
    }
    return builder.build();
  }

  private AuditTest parseTest(JsonObject obj) {
    AuditTest.Builder builder = AuditTest.newBuilder();
    Optional.ofNullable(obj.get("section"))
      .ifPresent(e -> builder.setSection(e.getAsString()));
    Optional.ofNullable(obj.get("type"))
      .ifPresent(e -> builder.setType(e.getAsString()));
    Optional.ofNullable(obj.get("desc"))
      .ifPresent(e -> builder.setDescription(e.getAsString()));
    if (obj.has("totals")) {
      builder.setTotals(parseTotals(obj.getAsJsonObject("totals")));
    }
    if (obj.has("results")) {
      JsonArray resultsArray = obj.getAsJsonArray("results");
      for (JsonElement elem : resultsArray) {
        builder.addResults(parseResult(elem.getAsJsonObject()));
      }
    }
    return builder.build();
  }

  private AuditResult parseResult(JsonObject obj) {
    AuditResult.Builder builder = AuditResult.newBuilder();
    Optional.ofNullable(obj.get("test_number"))
      .ifPresent(e -> builder.setTestNumber(e.getAsString()));
    Optional.ofNullable(obj.get("test_desc"))
      .ifPresent(e -> builder.setTestDescription(e.getAsString()));
    Optional.ofNullable(obj.get("audit"))
      .ifPresent(e -> builder.setAudit(e.getAsString()));
    Optional.ofNullable(obj.get("audit_env"))
      .ifPresent(e -> builder.setAuditEnv(e.getAsString()));
    Optional.ofNullable(obj.get("audit_config"))
      .ifPresent(e -> builder.setAuditConfig(e.getAsString()));
    Optional.ofNullable(obj.get("type"))
      .ifPresent(e -> builder.setType(e.getAsString()));
    Optional.ofNullable(obj.get("remediation"))
      .ifPresent(e -> builder.setRemediation(e.getAsString()));
    Optional.ofNullable(obj.get("test_info"))
      .ifPresent(e -> builder.setTestInfo(e.getAsString()));
    Optional.ofNullable(obj.get("status"))
      .ifPresent(e -> builder.setStatus(e.getAsString()));
    Optional.ofNullable(obj.get("actual_value"))
      .ifPresent(e -> builder.setActualValue(e.getAsString()));
    Optional.ofNullable(obj.get("scored"))
      .ifPresent(e -> builder.setScored(e.getAsString()));
    Optional.ofNullable(obj.get("is_multiple"))
      .ifPresent(e -> builder.setIsMultiple(e.getAsBoolean()));
    Optional.ofNullable(obj.get("expected_result"))
      .ifPresent(e -> builder.setExpectedResult(e.getAsString()));
    Optional.ofNullable(obj.get("reason"))
      .ifPresent(e -> builder.setReason(e.getAsString()));
    return builder.build();
  }

  private AuditTotals parseTotals(JsonObject obj) {
    AuditTotals.Builder builder = AuditTotals.newBuilder();
    Optional.ofNullable(obj.get("total_pass"))
      .ifPresent(e -> builder.setTotalPass(e.getAsInt()));
    Optional.ofNullable(obj.get("total_fail"))
      .ifPresent(e -> builder.setTotalFail(e.getAsInt()));
    Optional.ofNullable(obj.get("total_warn"))
      .ifPresent(e -> builder.setTotalWarn(e.getAsInt()));
    Optional.ofNullable(obj.get("total_info"))
      .ifPresent(e -> builder.setTotalInfo(e.getAsInt()));
    return builder.build();
  }
}
