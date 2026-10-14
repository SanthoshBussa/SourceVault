package com.sourcevault.job.repository;

import com.sourcevault.common.model.JobStatus;
import com.sourcevault.job.entity.CollectionJobEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CollectionJobRepository extends JpaRepository<CollectionJobEntity, String> {
    Page<CollectionJobEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<CollectionJobEntity> findTop10ByOrderByCreatedAtDesc();
    List<CollectionJobEntity> findByStatus(JobStatus status);
}
