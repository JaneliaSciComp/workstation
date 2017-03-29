package org.janelia.it.jacs.model.domain.interfaces;

public interface HasIdentifier {
    Number getId();
    void setId(Number id);
    default boolean hasId() {
        return getId() != null;
    }
}
