package org.janelia.jacs2.model.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.janelia.jacs2.model.BaseEntity;
import org.janelia.jacs2.model.Identifiable;
import org.janelia.jacs2.model.domain.annotations.MongoMapping;
import org.janelia.jacs2.utils.MongoObjectIdDeserializer;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

@MongoMapping(collectionName="subject", label="Subject")
public class Subject implements BaseEntity, Identifiable {
    public static final String ADMIN_KEY = "group:admin";
    public static final String USERS_KEY = "group:workstation_users";

    @JsonProperty("_id")
    @JsonDeserialize(using = MongoObjectIdDeserializer.class)
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

    @Override
    public String getEntityName() {
        return "Subject";
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

    public void addGroup(String group) {
        this.groups.add(group);
    }

    @JsonIgnore
    public boolean isAdmin() {
        return groups != null && groups.contains(ADMIN_KEY);
    }

    @JsonIgnore
    public Set<String> getSubjectClaims() {
        Set<String> claims = new LinkedHashSet<>();
        claims.add(key);
        for (String group : groups) {
            claims.add(group);
        }
        return claims;
    }
}
