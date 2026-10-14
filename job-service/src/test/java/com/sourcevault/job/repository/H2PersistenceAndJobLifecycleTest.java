package com.sourcevault.job.repository;

import com.sourcevault.common.model.CollectionMode;
import com.sourcevault.common.model.JobStatus;
import com.sourcevault.common.model.ResourceStatus;
import com.sourcevault.common.model.ResourceType;
import com.sourcevault.common.model.RetrievalMethod;
import com.sourcevault.job.entity.CollectedResourceEntity;
import com.sourcevault.job.entity.CollectionJobEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class H2PersistenceAndJobLifecycleTest {

    @Autowired
    private CollectionJobRepository jobRepository;

    @Autowired
    private CollectedResourceRepository resourceRepository;

    @Test
    void testPersistAndRetrieveJobAndResources() {
        String jobId = "job_" + UUID.randomUUID().toString().substring(0, 8);
        CollectionJobEntity job = new CollectionJobEntity();
        job.setId(jobId);
        job.setJobName("example_backup");
        job.setWebsiteUrl("https://example.com");
        job.setWebsiteOrigin("https://example.com");
        job.setCollectionMode(CollectionMode.URL_ONLY);
        job.setStatus(JobStatus.PENDING);
        job.setCreatedAt(Instant.now());

        jobRepository.save(job);

        // Update status to COLLECTING then COMPLETED
        job.setStatus(JobStatus.COLLECTING);
        job.setTotalDiscovered(3);
        job.setTotalDownloaded(3);
        jobRepository.save(job);

        CollectionJobEntity retrieved = jobRepository.findById(jobId).orElseThrow();
        assertThat(retrieved.getStatus()).isEqualTo(JobStatus.COLLECTING);
        assertThat(retrieved.getTotalDownloaded()).isEqualTo(3);

        // Add collected resources
        CollectedResourceEntity res1 = new CollectedResourceEntity();
        res1.setId(UUID.randomUUID().toString());
        res1.setJobId(jobId);
        res1.setOriginalUrl("https://example.com/index.html");
        res1.setResourceOrigin("https://example.com");
        res1.setResourcePath("/index.html");
        res1.setFileName("index.html");
        res1.setResourceType(ResourceType.HTML);
        res1.setStatus(ResourceStatus.DOWNLOADED);
        res1.setRetrievalMethod(RetrievalMethod.HTTP_PUBLIC);
        res1.setFileSize(1200L);
        resourceRepository.save(res1);

        List<CollectedResourceEntity> resources = resourceRepository.findByJobId(jobId);
        assertThat(resources).hasSize(1);
        assertThat(resources.get(0).getFileName()).isEqualTo("index.html");

        // Test filtering
        Page<CollectedResourceEntity> page = resourceRepository.findFiltered(
                jobId, ResourceStatus.DOWNLOADED, ResourceType.HTML, "index", PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(1);
    }
}
