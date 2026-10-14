package com.sourcevault.job.repository;

import com.sourcevault.common.model.ResourceStatus;
import com.sourcevault.common.model.ResourceType;
import com.sourcevault.job.entity.CollectedResourceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CollectedResourceRepository extends JpaRepository<CollectedResourceEntity, String> {
    List<CollectedResourceEntity> findByJobId(String jobId);

    Page<CollectedResourceEntity> findByJobId(String jobId, Pageable pageable);

    @Query("SELECT r FROM CollectedResourceEntity r WHERE r.jobId = :jobId " +
            "AND (:status IS NULL OR r.status = :status) " +
            "AND (:type IS NULL OR r.resourceType = :type) " +
            "AND (:search IS NULL OR LOWER(r.originalUrl) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(r.fileName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<CollectedResourceEntity> findFiltered(@Param("jobId") String jobId,
                                               @Param("status") ResourceStatus status,
                                               @Param("type") ResourceType type,
                                               @Param("search") String search,
                                               Pageable pageable);

    long countByJobIdAndStatus(String jobId, ResourceStatus status);

    void deleteByJobId(String jobId);
}
