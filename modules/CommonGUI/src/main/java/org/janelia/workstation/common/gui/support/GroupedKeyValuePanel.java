package org.janelia.workstation.common.gui.support;

import java.awt.Font;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

/**
 * A simple panel which lays out some components vertical with labels and optional group separators.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GroupedKeyValuePanel extends JPanel {

    private static final Font SEPARATOR_FONT = new Font("Sans Serif", Font.BOLD, 12);

    private final String separatorLabelConstraints = "split 2, span, gaptop 10lp, ay top";
    private final String separatorConstraints = "span 2, gaptop 12lp, grow";
    private final String componentConstraints;
    
    public GroupedKeyValuePanel() {
        this("wrap 2, ins 10, fillx", "[growprio 0]0[growprio 1, grow]", "");
    }

    public GroupedKeyValuePanel(String constraints, String columnConstraints, String rowConstraints) {
        this(constraints, columnConstraints, rowConstraints, "gap para, gaptop 10lp, ay top");
    }
    
    public GroupedKeyValuePanel(String constraints, String columnConstraints, String rowConstraints, String componentConstraints) {
        this.componentConstraints = componentConstraints;
        setLayout(new MigLayout(
                constraints,
                columnConstraints,
                rowConstraints
        ));
    }

    public void addSeparator() {
        add(new JSeparator(SwingConstants.HORIZONTAL), separatorConstraints);   
    }
    
    /**
     * Add a horizontal separator with a group label.
     * @param text
     */
    public JLabel addSeparator(String text) {
        JLabel label = new JLabel(text);
        label.setFont(SEPARATOR_FONT);
        add(label, separatorLabelConstraints);
        add(new JSeparator(SwingConstants.HORIZONTAL), separatorConstraints);
        return label;
    }

    /**
     * Add a component without a label which spans the entire width of the panel.
     * @param component the component to add
     */
    public JLabel addItem(JComponent component) {
        return addItem(null, component, "span 2");
    }

    /**
     * Add a component without a label which spans the entire width of the panel.
     * @param component the component to add
     * @param constraints additional MIG layout constraints
     */
    public JLabel addItem(JComponent component, String constraints) {
        String compConstraints = "span 2";
        if (!StringUtils.isEmpty(constraints)) {
            compConstraints += ", "+constraints;
        }
        return addItem(null, component, compConstraints);
    }

    /**
     * Add a component with a label.
     * @param label
     * @param component
     */
    public JLabel addItem(String label, JComponent component) {
        return addItem(label, component, "");
    }

    /**
     * Add a component with a label and additional constraints.
     * @param label
     * @param component
     * @param constraints
     */
    public JLabel addItem(String label, JComponent component, String constraints) {
        JLabel attrLabel = null;
        if (label!=null) {
            if (!StringUtils.isBlank(label)) {
                label = label + ": ";    
            }
            attrLabel = new JLabel(label);
            attrLabel.setLabelFor(component);
            add(attrLabel, componentConstraints);
        }
        String compConstraints = componentConstraints;
        if (!StringUtils.isEmpty(constraints)) {
            compConstraints += ", "+constraints;
        }
        add(component,compConstraints);
        return attrLabel;
    }
}
