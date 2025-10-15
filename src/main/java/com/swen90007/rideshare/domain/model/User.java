package com.swen90007.rideshare.domain.model;

/**
 * Base user of the system. Both Rider and Driver inherit from this.
 * role ∈ {"RIDER","DRIVER"} (kept as String to align with DB and controllers)
 */
public class User {
    private int id;
    private String name;
    private String email;
    private String password; // NOTE: keep consistent with your hashing/validation, if any
    private String role;     // "RIDER" or "DRIVER"

    public User() { }

    public User(int id, String name, String email, String password, String role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public int getId() { return id; }

    public void setId(int id) { this.id = id; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }

    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }

    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }

    public void setRole(String role) { this.role = role; }

    @Override
    public String toString() {
        return "User{id=" + id + ", name='" + name + '\'' +
                ", email='" + email + '\'' + ", role='" + role + '\'' + '}';
    }
}
