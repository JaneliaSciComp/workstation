package org.janelia.jacs2.model;

public interface BaseEntity {
    default String getEntityName() {
        return getClass().getSimpleName();
    };
}
