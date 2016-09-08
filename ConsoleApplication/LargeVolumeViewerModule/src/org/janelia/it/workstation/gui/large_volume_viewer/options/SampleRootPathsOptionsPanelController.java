package org.janelia.it.workstation.gui.large_volume_viewer.options;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import javax.swing.JComponent;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;

// This component is hidden for now, because the users are not sure if they want to use it. In the future it can either be removed or reinstated. 
//
//@OptionsPanelController.SubRegistration(
//        location = "LargeVolumeViewer",
//        displayName = "#AdvancedOption_DisplayName_SampleRootPaths",
//        keywords = "#AdvancedOption_Keywords_SampleRootPaths",
//        keywordsCategory = "LargeVolumeViewer/SampleRootPaths"
//)
//@org.openide.util.NbBundle.Messages({"AdvancedOption_DisplayName_SampleRootPaths=Sample Root Paths", "AdvancedOption_Keywords_SampleRootPaths=paths"})
public final class SampleRootPathsOptionsPanelController extends OptionsPanelController {

    private SampleRootPathsPanel panel;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private boolean changed;

    public void update() {
        getPanel().load();
        changed = false;
    }

    public void applyChanges() {
        getPanel().store();
        changed = false;
    }

    public void cancel() {
        // need not do anything special, if no changes have been persisted yet
    }

    public boolean isValid() {
        return getPanel().valid();
    }

    public boolean isChanged() {
        return changed;
    }

    public HelpCtx getHelpCtx() {
        return null; // new HelpCtx("...ID") if you have a help set
    }

    public JComponent getComponent(Lookup masterLookup) {
        return getPanel();
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }

    private SampleRootPathsPanel getPanel() {
        if (panel == null) {
            panel = new SampleRootPathsPanel(this);
        }
        return panel;
    }

    void changed() {
        if (!changed) {
            changed = true;
            pcs.firePropertyChange(OptionsPanelController.PROP_CHANGED, false, true);
        }
        pcs.firePropertyChange(OptionsPanelController.PROP_VALID, null, null);
    }

}
