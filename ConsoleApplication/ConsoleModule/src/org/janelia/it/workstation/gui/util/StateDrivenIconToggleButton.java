/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.util;

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

