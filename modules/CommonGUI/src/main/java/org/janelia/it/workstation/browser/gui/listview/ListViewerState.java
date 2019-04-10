package org.janelia.it.workstation.browser.gui.listview;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * The state of a list viewer of a given type.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "class")
public class ListViewerState {

    private ListViewerClassProvider type;

    @JsonCreator
    public ListViewerState(@JsonProperty("type") ListViewerClassProvider type) {
        this.type = type;
    }

    public ListViewerClassProvider getType() {
        return type;
    }
}
