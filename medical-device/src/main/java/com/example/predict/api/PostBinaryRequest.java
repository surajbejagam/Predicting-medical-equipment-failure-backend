package com.example.predict.api;

public class PostBinaryRequest {
  private String reason;
  private String action;
  private String actionSummary;
  private String dataNotes;
  private String deviceDescription;
  private String deviceName;
  private String deviceClassification;
  private String deviceRiskClass;

  public String getReason() { return reason; }
  public void setReason(String reason) { this.reason = reason; }
  public String getAction() { return action; }
  public void setAction(String action) { this.action = action; }
  public String getActionSummary() { return actionSummary; }
  public void setActionSummary(String actionSummary) { this.actionSummary = actionSummary; }
  public String getDataNotes() { return dataNotes; }
  public void setDataNotes(String dataNotes) { this.dataNotes = dataNotes; }
  public String getDeviceDescription() { return deviceDescription; }
  public void setDeviceDescription(String deviceDescription) { this.deviceDescription = deviceDescription; }
  public String getDeviceName() { return deviceName; }
  public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
  public String getDeviceClassification() { return deviceClassification; }
  public void setDeviceClassification(String deviceClassification) { this.deviceClassification = deviceClassification; }
  public String getDeviceRiskClass() { return deviceRiskClass; }
  public void setDeviceRiskClass(String deviceRiskClass) { this.deviceRiskClass = deviceRiskClass; }
}
