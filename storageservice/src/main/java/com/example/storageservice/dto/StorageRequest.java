package com.example.storageservice.dto;

import com.example.storageservice.model.StorageType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StorageRequest {

  @NotNull
  @JsonProperty("storageType")
  private StorageType storageType;
  @NotNull
  @JsonProperty("bucket")
  private String bucket;
  @NotNull
  @JsonProperty("path")
  private String path;

  public StorageRequest() {
  }

  public StorageType getStorageType() {
    return storageType;
  }

  public void setStorageType(StorageType storageType) {
    this.storageType = storageType;
  }

  public String getBucket() {
    return bucket;
  }

  public void setBucket(String bucket) {
    this.bucket = bucket;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }
}