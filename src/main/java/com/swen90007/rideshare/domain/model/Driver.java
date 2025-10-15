package com.swen90007.rideshare.domain.model;

/** Driver specialization. Extend with driver-only fields if needed. */
public class Driver extends User {
    public Driver() { super(); }
    public Driver(int id, String name, String email, String password) {
        super(id, name, email, password, "DRIVER");
    }
}
