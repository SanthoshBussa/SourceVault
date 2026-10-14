package com.sourcevault.job.repository;

import com.sourcevault.job.entity.ArchiveRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArchiveRecordRepository extends JpaRepository<ArchiveRecordEntity, String> {
    Optional<ArchiveRecordEntity> findByJobId(String jobId);
    void deleteByJobId(String jobId);
}
