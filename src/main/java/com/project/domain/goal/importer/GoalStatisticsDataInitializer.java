package com.project.domain.goal.importer;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

@Component
public class GoalStatisticsDataInitializer implements InitializingBean {

    private static final Logger log = LogManager.getLogger(GoalStatisticsDataInitializer.class);
    private static final String RESOURCE_PATTERN =
            "classpath*:data/wedding-statistics/*.csv";

    private final GoalStatisticsCsvImporter importer;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public GoalStatisticsDataInitializer(GoalStatisticsCsvImporter importer) {
        this.importer = importer;
    }

    @Override
    public void afterPropertiesSet() {
        if (!initialized.compareAndSet(false, true)) {
            return;
        }
        try {
            Resource[] resources =
                    new PathMatchingResourcePatternResolver().getResources(RESOURCE_PATTERN);
            for (Resource resource : resources) {
                importResource(resource);
            }
        } catch (IOException exception) {
            log.error("Failed to discover goal statistics CSV resources.", exception);
        }
    }

    private void importResource(Resource resource) {
        String category = extractCategory(resource.getFilename());
        try {
            Integer importedCount = importer.importResource(resource, category);
            log.info(
                    "Goal statistics CSV imported. category={}, rows={}",
                    category,
                    importedCount
            );
        } catch (Exception exception) {
            log.error(
                    "Failed to import goal statistics CSV. file={}, error={}",
                    resource.getFilename(),
                    exception.getClass().getSimpleName(),
                    exception
            );
        }
    }

    private String extractCategory(String fileName) {
        if (fileName == null || !fileName.toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("결혼 통계 CSV 파일명이 올바르지 않습니다.");
        }
        return fileName.substring(0, fileName.length() - 4).trim();
    }
}
