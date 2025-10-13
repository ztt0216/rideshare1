package com.unimelb.rideshare.domain.model;

import java.util.UUID;

/**
 * Rider participant that can raise ride requests.
 */
public final class Rider extends User {
    public Rider(UUID id, String name, String email) {
        super(id, name, email);
    }
}
