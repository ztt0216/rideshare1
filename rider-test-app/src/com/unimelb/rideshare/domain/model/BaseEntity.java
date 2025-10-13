package com.unimelb.rideshare.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Base entity that tracks an immutable identifier and simple audit metadata.
 */
public abstract class BaseEntity implements Identifiable {
    private final UUID id;
    private final Instant createdAt;
    private Instant updatedAt;

    protected BaseEntity(UUID id) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BaseEntity that = (BaseEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "id=" + id +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
