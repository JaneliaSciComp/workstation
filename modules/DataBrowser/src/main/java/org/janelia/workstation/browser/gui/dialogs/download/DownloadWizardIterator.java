package org.janelia.workstation.browser.gui.dialogs.download;

import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.janelia.workstation.integration.FrameworkImplProvider;
import org.openide.WizardDescriptor;
import org.openide.util.ChangeSupport;

/**
 * Download wizard that guides the user through several possible steps to choose what to download 
 * and how the output files are formatted.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public final class DownloadWizardIterator implements WizardDescriptor.Iterator<WizardDescriptor>, ChangeListener {

    public static final String PROP_WIZARD_STATE = "DownloadWizardStateProp";
    
    private ChangeSupport changeSupport = new ChangeSupport(this);
    private WizardDescriptor wiz;

    // State
    private List<WizardDescriptor.Panel<WizardDescriptor>> allPanels;
    private List<WizardDescriptor.Panel<WizardDescriptor>> seq3dPanels;
    private List<WizardDescriptor.Panel<WizardDescriptor>> seq2dPanels;
    private List<WizardDescriptor.Panel<WizardDescriptor>> currPanels;
    private String[] seq2dIndex;
    private String[] seq3dIndex;
    private int index;
    
    public void initialize(WizardDescriptor wizardDescriptor) {
        this.wiz = wizardDescriptor;
    }

    private void initializePanels() {
        if (allPanels == null) {
            
            DownloadWizardPanel1 panel1 = new DownloadWizardPanel1();
            DownloadWizardPanel2 panel2 = new DownloadWizardPanel2();
            DownloadWizardPanel3 panel3 = new DownloadWizardPanel3();

            // Listen to changes on each panel
            panel1.addChangeListener(this);
            panel2.addChangeListener(this);
            panel3.addChangeListener(this);
            
            allPanels = new ArrayList<WizardDescriptor.Panel<WizardDescriptor>>();
            allPanels.add(panel1);
            allPanels.add(panel2);
            allPanels.add(panel3);
            String[] steps = new String[allPanels.size()];

            Component mainFrame = FrameworkImplProvider.getMainFrame();
            
            for (int i = 0; i < allPanels.size(); i++) {
                Component c = allPanels.get(i).getComponent();
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

            seq3dIndex = new String[] { steps[0], steps[1], steps[2] };
            seq3dPanels = new ArrayList<WizardDescriptor.Panel<WizardDescriptor>>();
            seq3dPanels.add(panel1);
            seq3dPanels.add(panel2);
            seq3dPanels.add(panel3);

            seq2dIndex = new String[] { steps[0], steps[2] };
            seq2dPanels = new ArrayList<WizardDescriptor.Panel<WizardDescriptor>>();
            seq2dPanels.add(panel1);
            seq2dPanels.add(panel3);

            currPanels = seq3dPanels;   
        }
    }

    /**
     * If the user has not selected any 3d files for download, then there's 
     * no reason to show them the 3d file processing wizard panel.
     * @param has3d
     */
    private void setHas3d(boolean has3d) {
        
        List<WizardDescriptor.Panel<WizardDescriptor>> lastPanels = currPanels;
        String[] contentData;
        
        if (has3d) {
            currPanels = seq3dPanels;
            contentData = seq3dIndex;
        }
        else {
            currPanels = seq2dPanels;
            contentData = seq2dIndex;
        }

        wiz.putProperty(WizardDescriptor.PROP_CONTENT_DATA, contentData);
        
        if (lastPanels != currPanels) {
            fireChangeEvent();
        }
    }
    
    private void updateState() {
        DownloadWizardState state = (DownloadWizardState)wiz.getProperty(DownloadWizardIterator.PROP_WIZARD_STATE);
        if (state==null) {
            setHas3d(true);
        }
        else {
            if (state.has3d()) {
                setHas3d(true);
            }
            else {
                setHas3d(false);
            }
        }
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

    private final void fireChangeEvent() {
        changeSupport.fireChange();
    }
}
