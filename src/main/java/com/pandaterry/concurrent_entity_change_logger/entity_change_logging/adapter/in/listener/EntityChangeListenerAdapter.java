package com.pandaterry.concurrent_entity_change_logger.entity_change_logging.adapter.in.listener;

import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.application.service.LoggingService;
import com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.Operation;
import com.pandaterry.concurrent_entity_change_logger.shared.infrastructure.util.EntityStateCopier;
import com.pandaterry.concurrent_entity_change_logger.shared.config.EntityLoggingProperties;
import lombok.RequiredArgsConstructor;
import org.hibernate.event.spi.*;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EntityChangeListenerAdapter
        implements PostCommitInsertEventListener, PostCommitDeleteEventListener, PostCommitUpdateEventListener {
    private final LoggingService loggingService;
    private final EntityLoggingProperties loggingProperties;
    private final EntityStateCopier stateCopier;

    @Override
    public void onPostInsert(PostInsertEvent event) {
        if (loggingProperties.shouldLogChanges(event.getEntity())) {
            loggingService.logChange(null, event.getEntity(), Operation.CREATE);
        }
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        if (loggingProperties.shouldLogChanges(event.getEntity())) {
            Object oldEntity = stateCopier.cloneEntity(event);
            loggingService.logChange(oldEntity, event.getEntity(), Operation.UPDATE);
        }
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        if (loggingProperties.shouldLogChanges(event.getEntity())) {
            loggingService.logChange(event.getEntity(), null, Operation.DELETE);
        }
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        return true;
    }

    @Override
    public void onPostDeleteCommitFailed(PostDeleteEvent event) {

    }

    @Override
    public void onPostInsertCommitFailed(PostInsertEvent event) {

    }

    @Override
    public void onPostUpdateCommitFailed(PostUpdateEvent event) {

    }
}
