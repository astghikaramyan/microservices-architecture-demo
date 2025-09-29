package com.example.storageservice.dto;

import com.example.storageservice.model.StorageType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StorageResponse {
  @JsonProperty("id")
  private Long id;
  @JsonProperty("storageType")
  private StorageType storageType;
  @JsonProperty("bucket")
  private String bucket;
  @JsonProperty("path")
  private String path;

  public StorageResponse() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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