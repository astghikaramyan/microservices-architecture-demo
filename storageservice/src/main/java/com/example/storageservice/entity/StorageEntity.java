package com.example.storageservice.entity;

import com.example.storageservice.model.StorageType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "storage")
public class StorageEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "storage_seq")
  @SequenceGenerator(name = "storage_seq", sequenceName = "storage_seq", allocationSize = 1)
  private Integer id;
  @Enumerated(EnumType.STRING)
  @Column(name = "storage_type", nullable = false, length = 255)
  private StorageType storageType;
  @Column(nullable = false, unique = true)
  private String bucket;
  @Column(nullable = false, unique = true)
  private String path;

  public StorageEntity() {
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
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
