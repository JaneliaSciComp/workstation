package org.janelia.jacs2.model.domain;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "class")
public interface HasIdentifier {
    Number getId();
    void setId(Number id);
}
