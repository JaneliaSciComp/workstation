package org.janelia.it.workstation.browser.events.selection;

import org.janelia.it.workstation.browser.gui.colordepth.ColorDepthSearchesNode;

/**
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ColorDepthSearchesSelectionEvent extends NodeSelectionEvent {

    public ColorDepthSearchesSelectionEvent(Object source, ColorDepthSearchesNode node, boolean select, boolean clearAll, boolean isUserDriven) {
        super(source, node, select, clearAll, isUserDriven);
    }
    
    public ColorDepthSearchesNode getColorDepthMasksNode() {
        return (ColorDepthSearchesNode)getNode();
    }
}
