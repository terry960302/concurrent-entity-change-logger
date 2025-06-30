package com.pandaterry.concurrent_entity_change_logger.shared.infrastructure.mapper;

public abstract class BaseMapper<Domain, Jpo> {
    public abstract Domain toDomain(Jpo jpo);

    public abstract Jpo toEntity(Domain domain);
}
