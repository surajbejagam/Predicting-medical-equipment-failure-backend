package com.example.predict.db;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document(collection = "predictions")
public class PredictionResult {
  @Id
  private String id;

  private String task;
  private Map<String, Object> input;
  private Map<String, Object> output;
  private String scriptPath;
  private String modelPathUsed;
  private Integer exitCode;
  private String stderr;
  private Instant createdAt = Instant.now();

  @JsonProperty("_class")
  private final String className = "com.example.predict.db.PredictionResult";

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getTask() { return task; }
  public void setTask(String task) { this.task = task; }
  public Map<String, Object> getInput() { return input; }
  public void setInput(Map<String, Object> input) { this.input = input; }
  public Map<String, Object> getOutput() { return output; }
  public void setOutput(Map<String, Object> output) { this.output = output; }
  public String getScriptPath() { return scriptPath; }
  public void setScriptPath(String scriptPath) { this.scriptPath = scriptPath; }
  public String getModelPathUsed() { return modelPathUsed; }
  public void setModelPathUsed(String modelPathUsed) { this.modelPathUsed = modelPathUsed; }
  public Integer getExitCode() { return exitCode; }
  public void setExitCode(Integer exitCode) { this.exitCode = exitCode; }
  public String getStderr() { return stderr; }
  public void setStderr(String stderr) { this.stderr = stderr; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public String getClassName() { return className; }
}
