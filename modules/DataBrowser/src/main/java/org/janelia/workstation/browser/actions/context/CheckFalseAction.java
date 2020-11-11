package org.janelia.workstation.browser.actions.context;

import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

/**
 * Builds action to "uncheck" all the selected items.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */

@ActionID(
        category = "actions",
        id = "CheckFalseAction"
)
@ActionRegistration(
        displayName = "CTL_CheckFalseAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions", position = 21)
})
@NbBundle.Messages("CTL_CheckFalseAction=Uncheck Selected")
public class CheckFalseAction extends BaseCheckAction {
    CheckFalseAction() {
        super(false);
    }
}
