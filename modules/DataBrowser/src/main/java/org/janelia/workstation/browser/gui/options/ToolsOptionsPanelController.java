package org.janelia.workstation.browser.gui.options;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import javax.swing.JComponent;

import org.janelia.workstation.browser.tools.ToolMgr;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;

@OptionsPanelController.SubRegistration(
        location = "Core",
        id = ToolsOptionsPanelController.ID,
        displayName = "#AdvancedOption_DisplayName_Tools",
        keywords = "#AdvancedOption_Keywords_Tools",
        keywordsCategory = "Core/Tools",
        position=4
)
@org.openide.util.NbBundle.Messages({"AdvancedOption_DisplayName_Tools=Tools", "AdvancedOption_Keywords_Tools=tools vaa3d fiji"})
public final class ToolsOptionsPanelController extends OptionsPanelController {

    public static final String ID = "org.janelia.workstation.browser.gui.options.Tools";
    public static final String PATH = "Core/"+ID;
    
    private ToolsOptionsPanel panel;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private boolean changed;

    @Override
    public void update() {
        // This is inefficient, but it's the only way to get things to work without rewriting the entire broken ToolMgr
        ToolMgr.getToolMgr().reload();
        getPanel().load();
        changed = false;
    }

    @Override
    public void applyChanges() {
        getPanel().store();
        changed = false;
    }

    @Override
    public void cancel() {
        // need not do anything special, if no changes have been persisted yet
    }

    @Override
    public boolean isValid() {
        return getPanel().valid();
    }

    @Override
    public boolean isChanged() {
        return changed;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return null; // new HelpCtx("...ID") if you have a help set
    }

    @Override
    public JComponent getComponent(Lookup masterLookup) {
        return getPanel();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }

    private ToolsOptionsPanel getPanel() {
        if (panel == null) {
            panel = new ToolsOptionsPanel(this);
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
