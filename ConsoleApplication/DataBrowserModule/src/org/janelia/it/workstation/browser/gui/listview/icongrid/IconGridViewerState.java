package org.janelia.it.workstation.browser.gui.listview.icongrid;

import org.janelia.it.workstation.browser.gui.listview.ListViewerState;
import org.janelia.it.workstation.browser.gui.listview.ListViewerType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class IconGridViewerState extends ListViewerState {

    private int maxImageWidth;

    @JsonCreator
    public IconGridViewerState(@JsonProperty("maxImageWidth") int maxImageWidth) {
        super(ListViewerType.IconViewer);
        this.maxImageWidth = maxImageWidth;
    }

    public int getMaxImageWidth() {
        return maxImageWidth;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("IconGridViewerState[maxImageWidth=");
        builder.append(maxImageWidth);
        builder.append("]");
        return builder.toString();
    }
}