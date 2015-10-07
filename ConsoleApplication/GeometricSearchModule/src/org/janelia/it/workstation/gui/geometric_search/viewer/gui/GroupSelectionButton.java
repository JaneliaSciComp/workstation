package org.janelia.it.workstation.gui.geometric_search.viewer.gui;

import javax.swing.*;
import java.awt.*;

/**
 * Created by murphys on 9/29/2015.
 */
public class GroupSelectionButton extends JButton {

    public static final String A_TYPE = "A";
    public static final String N_TYPE = "-";
    public static final String S_TYPE = "S";

    boolean isSelected = false;

    @Override
    public boolean isSelected() {
        return isSelected;
    }

    @Override
    public void setSelected(boolean isSelected) {
        if (isSelected) {
            setForeground(new Color(255, 255, 0));
        } else {
            setForeground(new Color(100, 100, 100));
        }
        this.isSelected = isSelected;
    }

    public GroupSelectionButton(String name) {
        super(name);
        this.setFont(new Font("Arial", Font.BOLD, 9));
    }

    @Override
    public Color getForeground() {
        if (isSelected()) {
            return new Color(255, 255, 0);
        } else {
            return new Color(100, 100, 100);
        }
    }
}
