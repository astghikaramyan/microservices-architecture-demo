package com.example.storageservice.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public enum StorageType {
  STAGING("STAGING"),
  PERMANENT("PERMANENT");

  private final String storageType;

  StorageType(String storageType) {
    this.storageType = storageType;
  }

  @JsonValue
  public String getStorageType() {
    return storageType;
  }

  @JsonCreator
  public static StorageType fromValue(String value) {
    if (value == null) {
      return null;
    }
    try {
      return StorageType.valueOf(value.trim().toUpperCase());
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("Invalid StorageType: " + value);
    }
  }
}
