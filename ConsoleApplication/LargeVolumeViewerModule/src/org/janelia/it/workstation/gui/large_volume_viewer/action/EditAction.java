package org.janelia.it.workstation.gui.large_volume_viewer.action;

import javax.swing.AbstractAction;
import javax.swing.Icon;

import org.janelia.it.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponent;

/**
 * An abstract base class for editing actions in the LVV and Horta, which respects the editsAllowed() property of the LVV. 
 * Actions which extend this class can be used as global actions (by registration with the NetBeans API) and in context menus and buttons.
 * 
 * If an action is bound to a visible UI component, such as a button, then fireEnabledChangeEvent() needs to be called 
 * any time the editsAllowed() state changes on the LVV. Perhaps someday this could use an LVV event model, but for now this is
 * simpler.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class EditAction extends AbstractAction {

    public EditAction() {
        super();
    }

    public EditAction(String name, Icon icon) {
        super(name, icon);
    }

    public EditAction(String name) {
        super(name);
    }

    @Override
    public void setEnabled(boolean newValue) {
        throw new IllegalStateException("Calling setEnabled directly on EditAction is not supported");
    }

    @Override
    public boolean isEnabled() {
        return LargeVolumeViewerTopComponent.getInstance().editsAllowed();
    }
    
    public void fireEnabledChangeEvent() {
        boolean enabled = isEnabled();
        firePropertyChange("enabled", Boolean.valueOf(!enabled), Boolean.valueOf(enabled));
    }
}
