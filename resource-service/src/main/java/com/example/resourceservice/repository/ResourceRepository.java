package com.example.resourceservice.repository;


import com.example.resourceservice.entity.ResourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResourceRepository extends JpaRepository<ResourceEntity, Integer> {
}
