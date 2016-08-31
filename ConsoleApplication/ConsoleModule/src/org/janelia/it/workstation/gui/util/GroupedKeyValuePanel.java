package org.janelia.it.workstation.gui.util;

import java.awt.Font;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * A simple panel which lays out some components vertical with labels and optional group separators.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GroupedKeyValuePanel extends JPanel {

    private static final Font SEPARATOR_FONT = new Font("Sans Serif", Font.BOLD, 12);

    public GroupedKeyValuePanel() {
        setLayout(new MigLayout(
                "wrap 2, ins 10, fillx",
                "[growprio 0]0[growprio 1, grow]"
        ));
    }

    public void addItem(JComponent component) {
        addItem(null, component, "span 2");
    }

    public void addItem(String label, JComponent component) {
        addItem(label, component, "");
    }

    public void addItem(String label, JComponent component, String constraints) {
        if (label!=null) {
            JLabel attrLabel = new JLabel(label + ": ");
            attrLabel.setLabelFor(component);
            add(attrLabel, "gap para, gaptop 10lp, ay top");
        }
        String compConstraints = "gap para, gaptop 10lp, ay top";
        if (!StringUtils.isEmpty(constraints)) {
            compConstraints += ", "+constraints;
        }
        add(component,compConstraints);
    }


    public void addSeparator(String text) {
        JLabel label = new JLabel(text);
        label.setFont(SEPARATOR_FONT);
        add(label, "split 2, span, gaptop 10lp, ay top");
        add(new JSeparator(SwingConstants.HORIZONTAL), "wrap, gaptop 22lp, grow");
    }
}
