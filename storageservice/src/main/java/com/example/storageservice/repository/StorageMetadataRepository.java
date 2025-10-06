package com.example.storageservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.storageservice.entity.StorageEntity;
import com.example.storageservice.model.StorageType;

public interface StorageMetadataRepository extends JpaRepository<StorageEntity, Integer> {
  Optional<StorageEntity> findByStorageType(StorageType storageType);
}
