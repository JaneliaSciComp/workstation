package org.janelia.workstation.controller.options;

import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;

import javax.swing.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

@OptionsPanelController.SubRegistration(
        location = "Horta",
        displayName = "#AdvancedOption_DisplayName_Application",
        keywords = "#AdvancedOption_Keywords_Application",
        keywordsCategory = "LargeVolumeViewer/Application",
        position=1
)
@org.openide.util.NbBundle.Messages({"AdvancedOption_DisplayName_Application=Application", "AdvancedOption_Keywords_Application=LVV"})
public final class ApplicationOptionsPanelController extends OptionsPanelController {

    private ApplicationPanel panel;
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

    private ApplicationPanel getPanel() {
        if (panel == null) {
            panel = new ApplicationPanel(this);
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
