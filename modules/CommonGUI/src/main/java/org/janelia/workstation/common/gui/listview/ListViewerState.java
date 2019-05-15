package org.janelia.workstation.common.gui.listview;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * The state of a list viewer of a given type.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "class")
public class ListViewerState {

    // TODO: this won't deserialize for some reason... needs debugging before we add it back
    @JsonIgnore
    private ListViewerClassProvider type;

    @JsonCreator
    public ListViewerState() {
    }

//    @JsonCreator
    public ListViewerState(//@JsonProperty("type")
                           ListViewerClassProvider type) {
        this.type = null;
    }

    @JsonIgnore
    public ListViewerClassProvider getType() {
        return type;
    }
}
