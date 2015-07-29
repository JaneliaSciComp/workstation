package org.janelia.it.workstation.gui.geometric_search.search;

import javax.swing.*;
import java.awt.*;

/**
 * Created by murphys on 7/29/2015.
 */
public class GeometricSearchMenuPanel extends JPanel {

    GridLayout gridLayout = new GridLayout(1,10); // 1 row, 10 columns

    JLabel item1 = new JLabel("Item1");
    JLabel item2 = new JLabel("Item2");
    JLabel item3 = new JLabel("Item3");
    JLabel item4 = new JLabel("Item4");
    JLabel item5 = new JLabel("Item5");
    JLabel item6 = new JLabel("Item6");
    JLabel item7 = new JLabel("Item7");
    JLabel item8 = new JLabel("Item8");
    JLabel item9 = new JLabel("Item9");
    JLabel item10 = new JLabel("Item10");

    public GeometricSearchMenuPanel() {
        super();
        add(item1);
        add(item2);
        add(item3);
        add(item4);
        add(item5);
        add(item6);
        add(item7);
        add(item8);
        add(item9);
        add(item10);
    }

}
