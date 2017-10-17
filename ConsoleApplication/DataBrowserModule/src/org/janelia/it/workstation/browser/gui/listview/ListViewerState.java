package org.janelia.it.workstation.browser.gui.listview;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "class")
public class ListViewerState {

    private ListViewerType type;

    @JsonCreator
    public ListViewerState(@JsonProperty("type") ListViewerType type) {
        this.type = type;
    }

    public ListViewerType getType() {
        return type;
    }
}
