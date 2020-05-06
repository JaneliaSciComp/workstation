package org.janelia.workstation.browser.gui.dialogs.identifiers;

import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.WizardDescriptor;
import org.openide.util.ChangeSupport;

/**
 * Search wizard that guides the user through a batch search.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public final class IdentifiersWizardIterator implements WizardDescriptor.Iterator<WizardDescriptor>, ChangeListener {

    public static final String PROP_WIZARD_STATE = "IdentifiersWizardStateProp";
    
    private ChangeSupport changeSupport = new ChangeSupport(this);
    private WizardDescriptor wiz;

    // State
    private List<WizardDescriptor.Panel<WizardDescriptor>> currPanels;
    private int index;
    
    public void initialize(WizardDescriptor wizardDescriptor) {
        this.wiz = wizardDescriptor;
    }

    private void initializePanels() {
        if (currPanels == null) {
            
            IdentifiersWizardPanel1 panel1 = new IdentifiersWizardPanel1();
            IdentifiersWizardPanel2 panel2 = new IdentifiersWizardPanel2();

            // Listen to changes on each panel
            panel1.addChangeListener(this);
            
            currPanels = new ArrayList<>();
            currPanels.add(panel1);
            currPanels.add(panel2);
            String[] steps = new String[currPanels.size()];

            Component mainFrame = FrameworkAccess.getMainFrame();
            
            for (int i = 0; i < currPanels.size(); i++) {
                Component c = currPanels.get(i).getComponent();
                // Default step name to component name of panel.
                steps[i] = c.getName();
                if (c instanceof JComponent) { // assume Swing components
                    JComponent jc = (JComponent) c;
                    jc.putClientProperty(WizardDescriptor.PROP_CONTENT_SELECTED_INDEX, i);
                    jc.putClientProperty(WizardDescriptor.PROP_CONTENT_DATA, steps);
                    jc.putClientProperty(WizardDescriptor.PROP_AUTO_WIZARD_STYLE, true);
                    jc.putClientProperty(WizardDescriptor.PROP_CONTENT_DISPLAYED, true);
                    jc.putClientProperty(WizardDescriptor.PROP_CONTENT_NUMBERED, true);
                    jc.setPreferredSize(new Dimension((int) (mainFrame.getWidth() * 0.6), (int) (mainFrame.getHeight() * 0.6)));
                }
            }
        }
    }
    
    private void updateState() {
    }
    
    @Override
    public WizardDescriptor.Panel<WizardDescriptor> current() {
        initializePanels();
        return currPanels.get(index);
    }

    @Override
    public String name() {
        return "Step " + (index + 1) + " of " + currPanels.size();
    }

    @Override
    public boolean hasNext() {
        initializePanels();
        return index < currPanels.size() - 1;
    }

    @Override
    public boolean hasPrevious() {
        return index > 0;
    }

    @Override
    public void nextPanel() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        updateState();
        index++;
        wiz.putProperty(WizardDescriptor.PROP_CONTENT_SELECTED_INDEX, index);
    }

    @Override
    public void previousPanel() {
        if (!hasPrevious()) {
            throw new NoSuchElementException();
        }
        updateState();
        index--;
        wiz.putProperty(WizardDescriptor.PROP_CONTENT_SELECTED_INDEX, index);
    }
    
    @Override
    public void addChangeListener(ChangeListener l) {
        changeSupport.addChangeListener(l);
    }

    @Override
    public void removeChangeListener(ChangeListener l) {
        changeSupport.removeChangeListener(l);
    }

    /**
     * Listens to state change from panels.
     */
    @Override
    public void stateChanged(ChangeEvent e) {
        updateState();
        fireChangeEvent();
    }

    private void fireChangeEvent() {
        changeSupport.fireChange();
    }
}
