package org.janelia.jacs2.model.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeId;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import org.janelia.jacs2.utils.DomainUtils;
import org.janelia.jacs2.utils.MongoObjectIdDeserializer;

import java.math.BigInteger;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Every top-level "domain object" we store in MongoDB has a core set of attributes
 * which allow for identification (id/name) and permissions (owner/readers/writers)
 * as well as safe-updates with updatedDate.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "class")
public abstract class AbstractDomainObject implements DomainObject {
    @JsonProperty("_id")
    @JsonDeserialize(using = MongoObjectIdDeserializer.class)
    private Number id;
    private String name;
    private String ownerKey;
    private Set<String> readers = new HashSet<>();
    private Set<String> writers = new HashSet<>();
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ssX")
    private Date creationDate;
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ssX")
    private Date updatedDate;

    @JsonIgnore
    public String getOwnerName() {
        return DomainUtils.getNameFromSubjectKey(ownerKey);
    }

    @Override
    public Number getId() {
        return id;
    }

    @Override
    public void setId(Number id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getOwnerKey() {
        return ownerKey;
    }

    @Override
    public void setOwnerKey(String ownerKey) {
        this.ownerKey = ownerKey;
    }

    @Override
    public Set<String> getReaders() {
        return readers;
    }

    @Override
    public void setReaders(Set<String> readers) {
        Preconditions.checkArgument(readers != null, "Readers property cannot be null");
        this.readers = readers;
    }

    @Override
    public Set<String> getWriters() {
        return writers;
    }

    @Override
    public void setWriters(Set<String> writers) {
        Preconditions.checkArgument(writers != null, "Writers property cannot be null");
        this.writers = writers;
    }

    @Override
    public Date getCreationDate() {
        return creationDate;
    }

    @Override
    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    @Override
    public Date getUpdatedDate() {
        return updatedDate;
    }

    @Override
    public void setUpdatedDate(Date updatedDate) {
        this.updatedDate = updatedDate;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "#" + id;
    }
}
