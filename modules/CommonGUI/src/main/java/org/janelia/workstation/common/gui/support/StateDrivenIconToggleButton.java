package org.janelia.workstation.common.gui.support;

import javax.swing.Icon;
import javax.swing.JToggleButton;

/**
 * Changes the icon it presents, based on its toggled state.
 * 
 * @author fosterl
 */
public class StateDrivenIconToggleButton extends JToggleButton {

    private final Icon setIcon;
    private final Icon unsetIcon;

    public StateDrivenIconToggleButton(Icon setIcon, Icon unsetIcon) {
        this.setIcon = setIcon;
        this.unsetIcon = unsetIcon;
    }

    @Override
    public Icon getIcon() {
        if (this.isSelected()) {
            return setIcon;
        } else {
            return unsetIcon;
        }
    }
}

