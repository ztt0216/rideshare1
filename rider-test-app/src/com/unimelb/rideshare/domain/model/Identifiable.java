package com.unimelb.rideshare.domain.model;

import java.util.UUID;

/**
 * Contract for domain objects that carry an identity.
 */
public interface Identifiable {
    UUID getId();
}
