package org.janelia.it.FlyWorkstation.gui.framework.console;

import javax.swing.*;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 3:47 PM
 */
public class HelpMenu extends JMenu {
    public HelpMenu(Browser console) {
        super("Help");
        add(new JMenuItem("Call Christopher - x4662"));
        add(new JMenuItem("Call Konrad - x4242"));
        add(new JMenuItem("Call Sean   - x4324"));
        add(new JMenuItem("Call Todd   - x4696"));
        add(new JMenuItem("Call Yang   - x4626"));
    }
}
