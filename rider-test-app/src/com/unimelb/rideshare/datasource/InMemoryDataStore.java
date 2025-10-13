package com.unimelb.rideshare.datasource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory store backed by concurrent hash maps.
 */
public final class InMemoryDataStore implements DataStore {
    private final Map<String, Map<UUID, Map<String, Object>>> database = new ConcurrentHashMap<>();

    @Override
    public void save(String collection, UUID id, Map<String, Object> document) {
        database.computeIfAbsent(collection, key -> new ConcurrentHashMap<>()).put(id, cloneDocument(document));
    }

    @Override
    public void update(String collection, UUID id, Map<String, Object> document) {
        Map<UUID, Map<String, Object>> table = database.get(collection);
        if (table == null || !table.containsKey(id)) {
            throw new IllegalStateException("No document found to update for id " + id + " in " + collection);
        }
        table.put(id, cloneDocument(document));
    }

    @Override
    public Optional<Map<String, Object>> findById(String collection, UUID id) {
        Map<UUID, Map<String, Object>> table = database.get(collection);
        if (table == null) {
            return Optional.empty();
        }
        Map<String, Object> document = table.get(id);
        return Optional.ofNullable(document == null ? null : cloneDocument(document));
    }

    @Override
    public List<Map<String, Object>> findAll(String collection) {
        Map<UUID, Map<String, Object>> table = database.get(collection);
        if (table == null) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        table.values().forEach(doc -> result.add(cloneDocument(doc)));
        return result;
    }

    @Override
    public void delete(String collection, UUID id) {
        Map<UUID, Map<String, Object>> table = database.get(collection);
        if (table != null) {
            table.remove(id);
        }
    }

    private Map<String, Object> cloneDocument(Map<String, Object> document) {
        return new ConcurrentHashMap<>(document);
    }
}
