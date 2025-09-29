package com.example.storageservice.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import com.example.storageservice.dto.StorageRequest;
import com.example.storageservice.dto.StorageResponse;
import com.example.storageservice.entity.StorageEntity;

@Mapper(componentModel = "spring")
public interface StorageMapper {

  StorageEntity toEntity(StorageRequest request);

  StorageResponse toResponse(StorageEntity storageEntity);

  List<StorageResponse> toResponseList(List<StorageEntity> storageEntities);
}
