package com.campus.lostfound.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One row in the "users" table. @Entity tells Spring "turn this class
 * into a database table." Every field below becomes one column.
 *
 * IMPORTANT: @Table(name = "users") below is required — without it,
 * Spring would name the table "user" (singular), but "USER" is a
 * reserved word built into the H2 database itself, which breaks every
 * query. This is exactly the crash from your console log.
 *
 * There is no @Data, no Lombok, nothing invisible — every getter and
 * setter is written out below so you can see exactly what's happening.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // database assigns the id automatically (1, 2, 3...)
    private Long id;

    private String fullName;
    private String email;        // used as the login username
    private String password;     // stored as a SHA-256 hash, see PasswordUtil
    private boolean enabled = true;

    @Enumerated(EnumType.STRING) // store the role as text ("STUDENT") not a number, easier to read in the DB
    private Role role;

    // ---- getters and setters below. Boring on purpose — this is just
    // "give me the value" / "change the value", nothing clever. ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
