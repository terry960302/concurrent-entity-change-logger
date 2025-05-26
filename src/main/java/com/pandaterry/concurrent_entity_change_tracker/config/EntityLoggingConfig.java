package com.pandaterry.concurrent_entity_change_tracker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Set;

@Configuration
@ConfigurationProperties(prefix = "entity.logging")
public class EntityLoggingConfig {
    private boolean enableGlobalLogging = true;
    private Set<String> excludedEntities = new HashSet<>();

    public boolean isEnableGlobalLogging() {
        return enableGlobalLogging;
    }

    public void setEnableGlobalLogging(boolean enableGlobalLogging) {
        this.enableGlobalLogging = enableGlobalLogging;
    }

    public Set<String> getExcludedEntities() {
        return excludedEntities;
    }

    public void setExcludedEntities(Set<String> excludedEntities) {
        this.excludedEntities = excludedEntities;
    }
}