package com.sourcevault.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@ComponentScan(
        basePackages = {"com.sourcevault"},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {
                        com.sourcevault.analysis.WebsiteAnalysisApplication.class,
                        com.sourcevault.collection.ResourceCollectionApplication.class,
                        com.sourcevault.archive.ArchiveApplication.class,
                        com.sourcevault.job.JobApplication.class,
                        com.sourcevault.gateway.ApiGatewayApplication.class
                }
        )
)
@EntityScan(basePackages = {"com.sourcevault.job.entity"})
@EnableJpaRepositories(basePackages = {"com.sourcevault.job.repository"})
public class SourceVaultApplication {
    public static void main(String[] args) {
        SpringApplication.run(SourceVaultApplication.class, args);
    }
}
