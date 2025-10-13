package com.unimelb.rideshare.mapper;

import com.unimelb.rideshare.domain.model.Identifiable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Defines persistence operations for a domain type.
 */
public interface DataMapper<T extends Identifiable> {
    void insert(T entity);

    void update(T entity);

    void delete(UUID id);

    Optional<T> findById(UUID id);

    List<T> findAll();
}
