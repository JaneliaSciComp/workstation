package org.janelia.workstation.browser.gui.listview.table;

import org.janelia.workstation.common.gui.listview.ListViewerState;
import org.janelia.workstation.browser.gui.listview.ListViewerType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TableViewerState extends ListViewerState {

    private final int horizontalScrollValue;
    private final int verticalScrollValue;

    @JsonCreator
    public TableViewerState(
            @JsonProperty("horizontalScrollValue") int horizontalScrollValue, 
            @JsonProperty("verticalScrollValue") int verticalScrollValue) {
        super(ListViewerType.TableViewer);
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
        builder.append("TableViewerState [horizontalScrollValue=");
        builder.append(horizontalScrollValue);
        builder.append(", verticalScrollValue=");
        builder.append(verticalScrollValue);
        builder.append("]");
        return builder.toString();
    }
}