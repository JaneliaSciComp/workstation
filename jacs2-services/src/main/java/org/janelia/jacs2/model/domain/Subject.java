package org.janelia.jacs2.model.domain;

import org.janelia.jacs2.model.domain.annotations.MongoMapping;

import java.util.HashSet;
import java.util.Set;

@MongoMapping(collectionName="subject", label="Subject")
public class Subject {
    public static final String ADMIN_KEY = "group:admin";
    public static final String USERS_KEY = "group:workstation_users";

    private Number id;
    private String key;
    private String name;
    private String fullName;
    private String email;
    private Set<String> groups = new HashSet<>();

    public Number getId() {
        return id;
    }

    public void setId(Number id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Set<String> getGroups() {
        return groups;
    }

    public void setGroups(Set<String> groups) {
        this.groups = groups;
    }
}
