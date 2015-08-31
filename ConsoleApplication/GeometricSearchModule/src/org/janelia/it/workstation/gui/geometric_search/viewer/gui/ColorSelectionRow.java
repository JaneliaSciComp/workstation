package org.janelia.it.workstation.gui.geometric_search.viewer.gui;

import javax.swing.*;
import java.awt.*;

/**
 * Created by murphys on 8/28/2015.
 */
public class ColorSelectionRow extends JPanel {

    JCheckBox visibleCheckBox;
    JLabel nameLabel;

    public ColorSelectionRow(String name) {
        setName(name);
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setPreferredSize(new Dimension(150, 30));
        visibleCheckBox=new JCheckBox();
        visibleCheckBox.setSelected(true);
        nameLabel=new JLabel(name);
        add(visibleCheckBox);
        add(nameLabel);
    }

    public JCheckBox getVisibleCheckBox() {
        return visibleCheckBox;
    }

    public void setVisibleCheckBox(JCheckBox visibleCheckBox) {
        this.visibleCheckBox = visibleCheckBox;
    }
}
