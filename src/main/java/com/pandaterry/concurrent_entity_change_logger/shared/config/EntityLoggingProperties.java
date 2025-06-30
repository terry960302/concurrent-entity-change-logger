package com.pandaterry.concurrent_entity_change_logger.shared.config;

import com.pandaterry.concurrent_entity_change_logger.shared.domain.annotation.ExcludeFromLogging;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.HashSet;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "entity-logging")
@Getter
@Setter
public class EntityLoggingProperties {
    private boolean enabled = true;
    private Set<String> excludedEntities = new HashSet<>();
    private String storageType;
    private String processType;
    private Strategy strategy = new Strategy();

    @Value("${spring.jpa.properties.hibernate.jdbc.batch_size}")
    private int jpaBatchSize;

    @Getter
    @Setter
    public static class Strategy {
        private int queueSize = 100000;
        private int threadPoolSize = 5;
        private int flushInterval = 5000;
    }


    @PostConstruct
    public void validate() {
        Assert.isTrue(strategy.getQueueSize() > 0, "Queue size must be positive");
        Assert.isTrue(strategy.getThreadPoolSize() > 0, "Thread pool size must be positive");
        Assert.isTrue(strategy.getFlushInterval() > 0, "Flush interval must be positive");
        Assert.isTrue(jpaBatchSize > 0, "JPA batch size must be specified or must be positive");
    }

    public boolean shouldLogChanges(Object entity) {
        if (!enabled)
            return false;

        String entityName = entity.getClass().getSimpleName();
        if (excludedEntities.contains(entityName)) {
            return false;
        }

        return !entity.getClass().isAnnotationPresent(ExcludeFromLogging.class);
    }
}