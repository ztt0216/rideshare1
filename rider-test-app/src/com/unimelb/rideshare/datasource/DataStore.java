package com.unimelb.rideshare.datasource;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Abstracts the persistence mechanism used by the data mapper layer.
 */
public interface DataStore {
    void save(String collection, UUID id, Map<String, Object> document);

    void update(String collection, UUID id, Map<String, Object> document);

    Optional<Map<String, Object>> findById(String collection, UUID id);

    List<Map<String, Object>> findAll(String collection);

    void delete(String collection, UUID id);
}
