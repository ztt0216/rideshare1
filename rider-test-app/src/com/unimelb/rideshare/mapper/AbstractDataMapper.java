package com.unimelb.rideshare.mapper;

import com.unimelb.rideshare.datasource.DataStore;
import com.unimelb.rideshare.domain.model.Identifiable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Base class for data mapper implementations.
 * @param <T> domain type
 */
public abstract class AbstractDataMapper<T extends Identifiable> implements DataMapper<T> {
    private final DataStore dataStore;
    private final String collectionName;

    protected AbstractDataMapper(DataStore dataStore, String collectionName) {
        this.dataStore = dataStore;
        this.collectionName = collectionName;
    }

    @Override
    public void insert(T entity) {
        dataStore.save(collectionName, entity.getId(), toDocument(entity));
    }

    @Override
    public void update(T entity) {
        dataStore.update(collectionName, entity.getId(), toDocument(entity));
    }

    @Override
    public void delete(UUID id) {
        dataStore.delete(collectionName, id);
    }

    @Override
    public Optional<T> findById(UUID id) {
        return dataStore.findById(collectionName, id).map(this::fromDocument);
    }

    @Override
    public List<T> findAll() {
        return dataStore.findAll(collectionName).stream().map(this::fromDocument).collect(Collectors.toList());
    }

    protected abstract Map<String, Object> toDocument(T entity);

    protected abstract T fromDocument(Map<String, Object> document);
}
