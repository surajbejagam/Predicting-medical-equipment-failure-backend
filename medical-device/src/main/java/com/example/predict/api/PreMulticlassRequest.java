package com.example.predict.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class PreMulticlassRequest {
  private String deviceId;
  private String manufacturerId;

  @NotBlank private String riskClass;
  @NotBlank private String classification;
  @NotNull  private Boolean implanted;
  @NotNull  private Double quantityInCommerce;
  @NotBlank private String country;
  @NotBlank private String parentCompany;

  public String getDeviceId() { return deviceId; }
  public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
  public String getManufacturerId() { return manufacturerId; }
  public void setManufacturerId(String manufacturerId) { this.manufacturerId = manufacturerId; }
  public String getRiskClass() { return riskClass; }
  public void setRiskClass(String riskClass) { this.riskClass = riskClass; }
  public String getClassification() { return classification; }
  public void setClassification(String classification) { this.classification = classification; }
  public Boolean getImplanted() { return implanted; }
  public void setImplanted(Boolean implanted) { this.implanted = implanted; }
  public Double getQuantityInCommerce() { return quantityInCommerce; }
  public void setQuantityInCommerce(Double quantityInCommerce) { this.quantityInCommerce = quantityInCommerce; }
  public String getCountry() { return country; }
  public void setCountry(String country) { this.country = country; }
  public String getParentCompany() { return parentCompany; }
  public void setParentCompany(String parentCompany) { this.parentCompany = parentCompany; }
}
