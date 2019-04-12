package org.janelia.workstation.browser.gui.colordepth;

import javax.swing.Action;
import javax.swing.JMenuItem;

import org.janelia.workstation.common.actions.CopyToClipboardAction;
import org.janelia.workstation.common.gui.support.PopupContextMenu;
import org.janelia.workstation.core.model.SplitHalf;
import org.janelia.workstation.core.model.SplitTypeInfo;
import org.janelia.model.domain.enums.SplitHalfType;
import org.janelia.model.domain.gui.colordepth.ColorDepthMatch;
import org.janelia.model.domain.sample.Sample;

/**
 * Context pop up menu for split half annotations.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SplitHalfContextMenu extends PopupContextMenu {

    private ColorDepthResultImageModel imageModel;
    private ColorDepthMatch match;
    private SplitHalfType type;
    
    public SplitHalfContextMenu(ColorDepthResultImageModel imageModel, ColorDepthMatch match, SplitHalfType type) {
        this.imageModel = imageModel;
        this.match = match;
        this.type = type;
    }
    
    public void addMenuItems() {
        
        Sample sample = imageModel.getSample(match);
        SplitTypeInfo splitTypeInfo = imageModel.getSplitTypeInfo(sample);
        
        add(getDisabledItem("Available "+type+" split halves. Select to copy to clipboard:"));
        
        for (SplitHalf splitHalf : splitTypeInfo.getSplitHalves()) {
            if (splitHalf.getType()==type) {
                String title = getSplitHalfTitle(splitHalf);
                add(getNamedActionItem(new CopyToClipboardAction("Line",splitHalf.getLine()), title));
            }
        }
    }

    protected JMenuItem getNamedActionItem(Action action, String title) {
        return new JMenuItem(action) {
            @Override
            public String getText() {
                return "  "+title;
            }
        };
    }

    protected String getSplitHalfTitle(SplitHalf splitHalf) {
        StringBuilder sb = new StringBuilder();
        sb.append(splitHalf.getLine());
        sb.append(" (");
        sb.append(splitHalf.getSubcategory());
        sb.append(")");
        return sb.toString();
    }
}
