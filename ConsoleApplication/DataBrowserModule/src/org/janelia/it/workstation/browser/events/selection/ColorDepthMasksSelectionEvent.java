package org.janelia.it.workstation.browser.events.selection;

import org.janelia.it.workstation.browser.gui.colordepth.ColorDepthMasksNode;

/**
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ColorDepthMasksSelectionEvent extends NodeSelectionEvent {

    public ColorDepthMasksSelectionEvent(Object source, ColorDepthMasksNode node, boolean select, boolean clearAll, boolean isUserDriven) {
        super(source, node, select, clearAll, isUserDriven);
    }
    
    public ColorDepthMasksNode getColorDepthMasksNode() {
        return (ColorDepthMasksNode)getNode();
    }
}
