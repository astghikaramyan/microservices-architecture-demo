package com.example.resourceservice.model.storagemetadata;

public enum StorageType {
  STAGING("STAGING"),
  PERMANENT("PERMANENT");

  private String value;

  StorageType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }
}

