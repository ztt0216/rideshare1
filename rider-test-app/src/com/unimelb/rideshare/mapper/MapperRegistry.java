package com.unimelb.rideshare.mapper;

import com.unimelb.rideshare.domain.model.Identifiable;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple registry that maps domain types to their data mapper implementation.
 */
public final class MapperRegistry {
    private final Map<Class<?>, DataMapper<?>> mappers = new HashMap<>();

    public <T extends Identifiable> void register(Class<T> type, DataMapper<T> mapper) {
        mappers.put(type, mapper);
    }

    @SuppressWarnings("unchecked")
    public <T extends Identifiable> DataMapper<T> resolve(Class<T> type) {
        DataMapper<?> mapper = mappers.get(type);
        if (mapper == null) {
            throw new IllegalStateException("No mapper registered for type " + type.getName());
        }
        return (DataMapper<T>) mapper;
    }
}
