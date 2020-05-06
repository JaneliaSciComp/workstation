package org.janelia.workstation.site.jrc.action.context;

import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JOptionPane;

import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

/**
 * Allows the user to bind the "apply publishing names" action to a key or toolbar button.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "actions",
        id = "ApplyPublishingNamesAction"
)
@ActionRegistration(
        displayName = "#CTL_ApplyPublishingNamesAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/actions/Sample", position = 541),
        @ActionReference(path = "Shortcuts", name = "S-D-P")

})
@Messages("CTL_ApplyPublishingNamesAction=Apply Line Publishing Names")
public class ApplyPublishingNamesAction extends BaseContextualNodeAction {

    private Collection<Sample> samples = new ArrayList<>();

    @Override
    protected void processContext() {
        samples.clear();
        if (getNodeContext().isOnlyObjectsOfType(Sample.class)) {
            samples.addAll(getNodeContext().getOnlyObjectsOfType(Sample.class));
            setEnabledAndVisible(true);
        }
        else {
            setEnabledAndVisible(false);
        }
    }

    @Override
    public String getName() {
        if (samples!=null) {
            if (samples.size()>1) {
                return "Apply Line Publishing Names on "+samples.size()+" Samples";
            }
        }
        return super.getName();
    }

    @Override
    public void performAction() {

        Collection<Sample> samples = this.samples;
        if (samples==null || samples.isEmpty()) {
            JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
                    "In order to annotate the published line name, first select some Samples.");
            return;
        }

        try {
            ActivityLogHelper.logUserAction("ApplyPublishingNamesAction.actionPerformed");
            ApplyPublishingNamesActionListener a = new ApplyPublishingNamesActionListener(samples, true, false,true, FrameworkAccess.getMainFrame());
            a.actionPerformed(null);
        }
        catch (Exception ex) {
            FrameworkAccess.handleException("Problem setting publishing name", ex);
        }
    }

}
