package org.janelia.workstation.common.actions;

import java.awt.event.ActionEvent;
import java.util.Collection;

import javax.swing.AbstractAction;

import org.apache.commons.lang3.StringUtils;
import org.janelia.model.domain.interfaces.HasName;

/**
 * Display a grey label indicating how many objects are selected.
 *
 * @deprecated use ContextualLabelAction instead
 * @see ContextualLabelAction
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@Deprecated
public class PopupLabelAction extends AbstractAction {

    private Collection objects;

    public PopupLabelAction(Collection objects) {
        super(getName(objects));
    }

    private static String getName(Collection objects) {
        if (objects.size()>1) {
            return "(Multiple selected)";
        }
        else {

            Object lastSelectedObject = null;
            for (Object object : objects) {
                lastSelectedObject = object;
            }

            if (lastSelectedObject != null) {
                String name;
                if (lastSelectedObject instanceof HasName) {
                    HasName named = (HasName) lastSelectedObject;
                    name = named.getName();
                }
                else {
                    name = lastSelectedObject.toString();
                }
                return StringUtils.abbreviate(name, 50);
            }
        }
        return "(Nothing selected)";
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
    }
}