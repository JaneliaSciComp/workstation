package org.janelia.it.workstation.browser.gui.colordepth;

import org.janelia.workstation.common.gui.listview.ListViewerState;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ColorDepthResultTableViewerState extends ListViewerState {

    private final int horizontalScrollValue;
    private final int verticalScrollValue;

    @JsonCreator
    public ColorDepthResultTableViewerState(
            @JsonProperty("horizontalScrollValue") int horizontalScrollValue, 
            @JsonProperty("verticalScrollValue") int verticalScrollValue) {
        super(ColorDepthListViewerType.ColorDepthResultTableViewer);
        this.horizontalScrollValue = horizontalScrollValue;
        this.verticalScrollValue = verticalScrollValue;
    }

    public int getHorizontalScrollValue() {
        return horizontalScrollValue;
    }

    public int getVerticalScrollValue() {
        return verticalScrollValue;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ColorDepthResultTableViewerState[horizontalScrollValue=");
        builder.append(horizontalScrollValue);
        builder.append(", verticalScrollValue=");
        builder.append(verticalScrollValue);
        builder.append("]");
        return builder.toString();
    }
}