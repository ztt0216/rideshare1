package com.unimelb.rideshare.unitofwork;

import com.unimelb.rideshare.domain.model.Identifiable;
import com.unimelb.rideshare.mapper.DataMapper;
import com.unimelb.rideshare.mapper.MapperRegistry;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tracks changes to aggregates so they can be persisted in a single transaction boundary.
 */
public final class UnitOfWork {
    private final MapperRegistry mapperRegistry;
    private final Map<Identifiable, Class<? extends Identifiable>> newEntities = new IdentityHashMap<>();
    private final Map<Identifiable, Class<? extends Identifiable>> dirtyEntities = new IdentityHashMap<>();
    private final Map<Identifiable, Class<? extends Identifiable>> removedEntities = new IdentityHashMap<>();

    public UnitOfWork(MapperRegistry mapperRegistry) {
        this.mapperRegistry = mapperRegistry;
    }

    public <T extends Identifiable> void registerNew(T entity, Class<T> type) {
        newEntities.put(entity, type);
    }

    public <T extends Identifiable> void registerDirty(T entity, Class<T> type) {
        if (!newEntities.containsKey(entity)) {
            dirtyEntities.put(entity, type);
        }
    }

    public <T extends Identifiable> void registerRemoved(T entity, Class<T> type) {
        if (newEntities.remove(entity) != null) {
            return;
        }
        dirtyEntities.remove(entity);
        removedEntities.put(entity, type);
    }

    public void commit() {
        flushNew();
        flushDirty();
        flushRemoved();
        clear();
    }

    private void flushNew() {
        new ArrayList<>(newEntities.entrySet()).forEach(entry -> {
            Identifiable entity = entry.getKey();
            Class<? extends Identifiable> type = entry.getValue();
            mapperOf(type).insert(entity);
        });
    }

    private void flushDirty() {
        new ArrayList<>(dirtyEntities.entrySet()).forEach(entry -> {
            Identifiable entity = entry.getKey();
            Class<? extends Identifiable> type = entry.getValue();
            mapperOf(type).update(entity);
        });
    }

    private void flushRemoved() {
        new ArrayList<>(removedEntities.entrySet()).forEach(entry -> {
            Identifiable entity = entry.getKey();
            Class<? extends Identifiable> type = entry.getValue();
            mapperOf(type).delete(entity.getId());
        });
    }

    private void clear() {
        newEntities.clear();
        dirtyEntities.clear();
        removedEntities.clear();
    }

    @SuppressWarnings("unchecked")
    private <T extends Identifiable> DataMapper<T> mapperOf(Class<? extends Identifiable> type) {
        return (DataMapper<T>) mapperRegistry.resolve((Class<T>) type);
    }
}
