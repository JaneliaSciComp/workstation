package org.janelia.workstation.browser.gui.editor;

import javax.swing.JLabel;
import javax.swing.JMenuItem;

import org.janelia.workstation.common.gui.support.PopupContextMenu;
import org.janelia.workstation.common.actions.CopyToClipboardAction;

/**
 * Right-click context menu for labels.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class LabelContextMenu extends PopupContextMenu {

    private final String name;
    private final JLabel label;

    public LabelContextMenu(String name, JLabel label) {
        this.name = name;
        this.label = label;
    }
    
    public void addMenuItems() {
        add(getTitleItem());
        add(getCopyNameToClipboardItem());
    }

    protected JMenuItem getTitleItem() {
        JMenuItem titleMenuItem = new JMenuItem(label.getText());
        titleMenuItem.setEnabled(false);
        return titleMenuItem;
    }
    
    protected JMenuItem getCopyNameToClipboardItem() {
        return getNamedActionItem(new CopyToClipboardAction(name,label.getText()));
    }
}
