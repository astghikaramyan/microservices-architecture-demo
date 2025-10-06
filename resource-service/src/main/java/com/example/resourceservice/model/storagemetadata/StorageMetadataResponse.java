package com.example.resourceservice.model.storagemetadata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StorageMetadataResponse {
  private Long id;
  private StorageType storageType;
  private String bucket;
  private String path;
}
