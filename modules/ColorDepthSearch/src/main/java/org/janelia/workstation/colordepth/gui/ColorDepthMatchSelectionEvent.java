package org.janelia.workstation.colordepth.gui;

import java.util.List;

import org.janelia.model.domain.gui.cdmip.ColorDepthMatch;
import org.janelia.workstation.core.events.selection.ObjectSelectionEvent;

/**
 * Event that is thrown when a color depth match is selected in the Color Depth Search editor.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ColorDepthMatchSelectionEvent extends ObjectSelectionEvent<ColorDepthMatch> {

    public ColorDepthMatchSelectionEvent(Object source, List<? extends ColorDepthMatch> matches, boolean select, boolean clearAll, boolean isUserDriven) {
        super(source, matches, select, clearAll, isUserDriven);
    }

    public Object getSource() {
        return getSourceComponent();
    }

    public List<ColorDepthMatch> getMatches() {
        return getObjects();
    }
}
