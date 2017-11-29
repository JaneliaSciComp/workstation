package org.janelia.it.workstation.browser.model.descriptors;

import java.util.ArrayList;
import java.util.List;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.interfaces.HasFiles;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class SelfArtifactDescriptor extends ArtifactDescriptor {

    // Empty constructor needed for JSON deserialization
    public SelfArtifactDescriptor() {
    }

    @JsonIgnore
    public String getObjective() {
        return null;
    }

    @JsonIgnore
    public String getArea() {
        return null;
    }

    @JsonIgnore
    public boolean isAligned() {
        return false;
    }

    @JsonIgnore
    public List<DomainObject> getDescribedObjects(DomainObject sourceObject) throws Exception {
        List<DomainObject> objects = new ArrayList<>();
        objects.add(sourceObject);
        return objects;
    }

    @JsonIgnore
    public List<HasFiles> getFileSources(DomainObject sourceObject) throws Exception {
        List<HasFiles> objects = new ArrayList<>();
        if (sourceObject instanceof HasFiles) {
            objects.add((HasFiles)sourceObject);
        }
        return objects;
    }
    
    @Override
    public String toString() {
        return "Selected Object";
    }

    @Override
    public int hashCode() {
        return SelfArtifactDescriptor.class.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof SelfArtifactDescriptor);
    }
}