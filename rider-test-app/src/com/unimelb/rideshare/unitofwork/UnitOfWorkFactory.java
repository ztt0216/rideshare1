package com.unimelb.rideshare.unitofwork;

import com.unimelb.rideshare.mapper.MapperRegistry;

/**
 * Creates unit of work instances so services can obtain scoped persistence sessions.
 */
public final class UnitOfWorkFactory {
    private final MapperRegistry mapperRegistry;

    public UnitOfWorkFactory(MapperRegistry mapperRegistry) {
        this.mapperRegistry = mapperRegistry;
    }

    public UnitOfWork create() {
        return new UnitOfWork(mapperRegistry);
    }
}
