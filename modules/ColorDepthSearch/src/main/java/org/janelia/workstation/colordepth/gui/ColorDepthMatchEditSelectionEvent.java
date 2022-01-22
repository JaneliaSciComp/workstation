package org.janelia.workstation.colordepth;

import java.util.List;

import org.janelia.model.domain.gui.cdmip.ColorDepthMatch;

/**
 * Event that is thrown when a color depth match is "edit selected" with a checkbox in the Color Depth Search editor.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ColorDepthMatchEditSelectionEvent {

    private final Object source;
    private final List<ColorDepthMatch> matches;
    private final boolean select;
    private final boolean clearAll;
    private final boolean isUserDriven;
    
    public ColorDepthMatchEditSelectionEvent(Object source, List<ColorDepthMatch> matches, boolean select, boolean clearAll, boolean isUserDriven) {
        this.source = source;
        this.matches = matches;
        this.select = select;
        this.clearAll = clearAll;
        this.isUserDriven = isUserDriven;
    }

    public Object getSource() {
        return source;
    }

    public ColorDepthMatch getObjectIfSingle() {
        return matches.size()==1 ? matches.get(0) : null;
    }

    public List<ColorDepthMatch> getMatches() {
        return matches;
    }
    
    public boolean isSelect() {
        return select;
    }

    public boolean isClearAll() {
        return clearAll;
    }

    public boolean isUserDriven() {
        return isUserDriven;
    }

    @Override
    public String toString() {
        return "ColorDepthMatchEditSelectionEvent[" + "source=" + source.getClass().getSimpleName() + 
                ", matches=" + matches + ']';
    }
}
