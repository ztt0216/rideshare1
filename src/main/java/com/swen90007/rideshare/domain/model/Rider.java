package com.swen90007.rideshare.domain.model;

/** Rider specialization. Extend with rider-only fields if needed. */
public class Rider extends User {
    public Rider() { super(); }
    public Rider(int id, String name, String email, String password) {
        super(id, name, email, password, "RIDER");
    }
}
