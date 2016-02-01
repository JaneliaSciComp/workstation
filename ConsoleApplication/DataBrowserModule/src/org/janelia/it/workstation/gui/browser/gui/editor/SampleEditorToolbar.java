package org.janelia.it.workstation.gui.browser.gui.editor;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.UIManager;

import de.javasoft.swing.SimpleDropDownButton;

/**
 * Tool bar for the sample editor panel.
 * 
 * @see org.janelia.it.workstation.gui.browser.gui.editor.SampleEditorPanel
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleEditorToolbar extends JPanel {

    protected JToolBar toolbar;
    private final SimpleDropDownButton viewButton;
    private final SimpleDropDownButton objectiveButton;
    private final SimpleDropDownButton areaButton;

    public SampleEditorToolbar() {
        super(new BorderLayout());

        toolbar = new JToolBar();
        toolbar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, (Color) UIManager.get("windowBorder")), BorderFactory.createEmptyBorder(0, 5, 2, 5)));
        toolbar.setFloatable(false);
        toolbar.setRollover(true);
        add(toolbar);

        viewButton = new SimpleDropDownButton("View: ");
        toolbar.add(viewButton);
        
        objectiveButton = new SimpleDropDownButton("Objective: ");
        toolbar.add(objectiveButton);
        
        areaButton = new SimpleDropDownButton("Area: ");
        toolbar.add(areaButton);
    }

    public SimpleDropDownButton getViewButton() {
        return viewButton;
    }
    
    public SimpleDropDownButton getObjectiveButton() {
        return objectiveButton;
    }

    public SimpleDropDownButton getAreaButton() {
        return areaButton;
    }
    
}
