package org.janelia.workstation.browser.gui.colordepth;

import org.janelia.workstation.common.gui.listview.ListViewerState;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ColorDepthResultIconGridViewerState extends ListViewerState {

    private int maxImageWidth;

    @JsonCreator
    public ColorDepthResultIconGridViewerState(@JsonProperty("maxImageWidth") int maxImageWidth) {
        super(ColorDepthListViewerType.ColorDepthResultImageViewer);
        this.maxImageWidth = maxImageWidth;
    }

    public int getMaxImageWidth() {
        return maxImageWidth;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ColorDepthResultIconGridViewerState[maxImageWidth=");
        builder.append(maxImageWidth);
        builder.append("]");
        return builder.toString();
    }
}