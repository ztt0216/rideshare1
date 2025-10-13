package com.unimelb.rideshare.mapper;

import com.unimelb.rideshare.datasource.DataStore;
import com.unimelb.rideshare.domain.model.Rider;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Mapper for rider entities.
 */
public final class RiderDataMapper extends AbstractDataMapper<Rider> {
    public static final String COLLECTION = "riders";

    public RiderDataMapper(DataStore dataStore) {
        super(dataStore, COLLECTION);
    }

    @Override
    protected Map<String, Object> toDocument(Rider entity) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("id", entity.getId());
        doc.put("name", entity.getName());
        doc.put("email", entity.getEmail());
        return doc;
    }

    @Override
    protected Rider fromDocument(Map<String, Object> document) {
        UUID id = (UUID) document.get("id");
        String name = (String) document.get("name");
        String email = (String) document.get("email");
        return new Rider(id, name, email);
    }
}
