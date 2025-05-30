package com.pandaterry.concurrent_entity_change_logger.core.listener;

import com.pandaterry.concurrent_entity_change_logger.core.enumerated.OperationType;
import com.pandaterry.concurrent_entity_change_logger.core.strategy.LoggingStrategy;
import com.pandaterry.concurrent_entity_change_logger.core.util.EntityStateCopier;
import com.pandaterry.concurrent_entity_change_logger.core.util.EntityLoggingCondition;
import lombok.RequiredArgsConstructor;
import org.hibernate.event.spi.*;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EntityChangeListener
        implements PostInsertEventListener, PostUpdateEventListener, PostDeleteEventListener {
    private final LoggingStrategy loggingStrategy;
    private final EntityLoggingCondition loggingCondition;
    private final EntityStateCopier stateCopier;

    @Override
    public void onPostInsert(PostInsertEvent event) {
        if (loggingCondition.shouldLogChanges(event.getEntity())) {
            loggingStrategy.logChange(null, event.getEntity(), OperationType.INSERT);
        }
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        if (loggingCondition.shouldLogChanges(event.getEntity())) {
            Object oldEntity = stateCopier.cloneEntity(event);
            loggingStrategy.logChange(oldEntity, event.getEntity(), OperationType.UPDATE);
        }
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        if (loggingCondition.shouldLogChanges(event.getEntity())) {
            loggingStrategy.logChange(event.getEntity(), null, OperationType.DELETE);
        }
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        return false;
    }
}
