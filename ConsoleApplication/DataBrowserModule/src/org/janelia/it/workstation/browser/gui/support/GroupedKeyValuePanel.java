package org.janelia.it.workstation.browser.gui.support;

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
        this(null);
    }
            
    public GroupedKeyValuePanel(String rowConstraints) {
        this("wrap 2, ins 10, fill", "[growprio 0, fill]0[growprio 1, grow, fill]", rowConstraints);
    }
    
    public GroupedKeyValuePanel(String layoutConstraints, String columnConstraints, String rowConstraints) {
        setLayout(new MigLayout(layoutConstraints, columnConstraints, rowConstraints));
    }

    /**
     * Add a horizontal separator with a group label.
     * @param text
     */
    public void addSeparator(String text) {
        JLabel label = new JLabel(text);
        label.setFont(SEPARATOR_FONT);
        add(label, "split 2, span, gaptop 10lp, ay top");
        add(new JSeparator(SwingConstants.HORIZONTAL), "wrap, gaptop 22lp, grow");
    }

    /**
     * Add a component without a label which spans the entire width of the panel.
     * @param component the component to add
     */
    public void addItem(JComponent component) {
        addItem(null, component, "span 2");
    }

    /**
     * Add a component without a label which spans the entire width of the panel.
     * @param component the component to add
     * @param constraints additional MIG layout constraints
     */
    public void addItem(JComponent component, String constraints) {
        String compConstraints = addConstraints("span 2", constraints);
        addItem(null, component, compConstraints);
    }

    private String addConstraints(String staticConstraints, String additionalConstraints) {
        StringBuilder compConstraints = new StringBuilder(staticConstraints);
        if (!StringUtils.isEmpty(additionalConstraints)) {
            compConstraints.append(", ").append(additionalConstraints);
        }
        return compConstraints.toString();
    }
    
    /**
     * Add a component with a label.
     * @param label
     * @param component
     */
    public void addItem(String label, JComponent component) {
        addItem(label, component, "");
    }

    /**
     * Add a component with a label and additional constraints.
     * @param label
     * @param component
     * @param constraints
     */
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
}
