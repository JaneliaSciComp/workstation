package org.janelia.workstation.browser.actions.context;

import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

/**
 * Builds action to "check" all the selected items.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "actions",
        id = "CheckTrueAction"
)
@ActionRegistration(
        displayName = "CTL_CheckTrueAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions", position = 20)
})
@NbBundle.Messages("CTL_CheckTrueAction=Check Selected")
public class CheckTrueAction extends BaseCheckAction {
    CheckTrueAction() {
        super(true);
    }
}
