package com.example.predict.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PreMulticlassRequest {
  private String deviceName;
  private String manufacturerName;
  @NotBlank private String riskClass;
  @NotBlank private String classification;
  @NotNull  private Boolean implanted;
  @NotNull  private Double quantityInCommerce;
  @NotBlank private String country;
  @NotBlank private String parentCompany;


}
