package com.pandaterry.concurrent_entity_change_logger.core.infrastructure.listener;

import com.pandaterry.concurrent_entity_change_logger.core.domain.enumerated.OperationType;
import com.pandaterry.concurrent_entity_change_logger.core.application.strategy.LoggingStrategy;
import com.pandaterry.concurrent_entity_change_logger.core.shared.util.EntityStateCopier;
import com.pandaterry.concurrent_entity_change_logger.core.infrastructure.config.EntityLoggingProperties;
import lombok.RequiredArgsConstructor;
import org.hibernate.event.spi.*;
import org.hibernate.persister.entity.EntityPersister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EntityChangeListener
        implements PostCommitUpdateEventListener, PostCommitDeleteEventListener, PostCommitInsertEventListener {
    private final LoggingStrategy loggingStrategy;
    private final EntityLoggingProperties loggingProperties;
    private final EntityStateCopier stateCopier;
    private final Logger log = LoggerFactory.getLogger(EntityChangeListener.class);
    
    @Override
    public void onPostInsert(PostInsertEvent event) {
        if (loggingProperties.shouldLogChanges(event.getEntity())) {
            loggingStrategy.logChange(null, event.getEntity(), OperationType.INSERT);
        }
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        if (loggingProperties.shouldLogChanges(event.getEntity())) {
            Object oldEntity = stateCopier.cloneEntity(event);
            loggingStrategy.logChange(oldEntity, event.getEntity(), OperationType.UPDATE);
        }
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        if (loggingProperties.shouldLogChanges(event.getEntity())) {
            loggingStrategy.logChange(event.getEntity(), null, OperationType.DELETE);
        }
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        return false;
    }

    @Override
    public void onPostInsertCommitFailed(PostInsertEvent event) {
        log.error("Failed to insert entity: {}", event.getEntity());
    }

    @Override
    public void onPostDeleteCommitFailed(PostDeleteEvent event) {
        log.error("Failed to delete entity: {}", event.getEntity());
    }

    @Override
    public void onPostUpdateCommitFailed(PostUpdateEvent event) {
        log.error("Failed to update entity: {}", event.getEntity());
    }
}
