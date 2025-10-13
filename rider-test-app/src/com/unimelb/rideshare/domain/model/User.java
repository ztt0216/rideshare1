package com.unimelb.rideshare.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Base abstraction for a platform participant.
 */
public abstract class User extends BaseEntity {
    private final String name;
    private final String email;

    protected User(UUID id, String name, String email) {
        super(id);
        this.name = requireNonBlank(name, "name");
        this.email = requireNonBlank(email, "email");
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        return value;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "id=" + getId() +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof User)) {
            return false;
        }
        User user = (User) o;
        return Objects.equals(getId(), user.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
