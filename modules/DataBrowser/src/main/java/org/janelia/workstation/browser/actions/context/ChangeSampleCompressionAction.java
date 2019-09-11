package org.janelia.workstation.browser.actions.context;

import java.util.ArrayList;
import java.util.Collection;

import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.browser.gui.dialogs.ChangeSampleCompressionDialog;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "Actions",
        id = "ChangeSampleCompressionAction"
)
@ActionRegistration(
        displayName = "#CTL_ChangeSampleCompressionAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions/Sample", position = 520)
})
@NbBundle.Messages("CTL_ChangeSampleCompressionAction=Change Sample Compression Strategy...")
public class ChangeSampleCompressionAction extends BaseContextualNodeAction {

    private Collection<Sample> samples = new ArrayList<>();

    @Override
    protected void processContext() {
        samples.clear();
        setVisible(false);
        if (getNodeContext().isOnlyObjectsOfType(Sample.class)) {
            for (Sample sample : getNodeContext().getOnlyObjectsOfType(Sample.class)) {
                setVisible(true);
                if (ClientDomainUtils.hasWriteAccess(sample)) {
                    samples.add(sample);
                }
            }
        }
        setEnabled(!samples.isEmpty());
    }

    @Override
    public void performAction() {
        Collection<Sample> samples = new ArrayList<>(this.samples);
        ChangeSampleCompressionDialog dialog = new ChangeSampleCompressionDialog();
        dialog.showForSamples(samples);
    }

}