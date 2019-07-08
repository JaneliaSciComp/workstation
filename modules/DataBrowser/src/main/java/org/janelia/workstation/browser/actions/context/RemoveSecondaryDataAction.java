package org.janelia.workstation.browser.actions.context;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JMenuItem;

import org.janelia.it.jacs.shared.utils.Constants;
import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.browser.gui.dialogs.SecondaryDataRemovalDialog;
import org.janelia.workstation.common.actions.BaseContextualPopupAction;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.util.Utils;
import org.janelia.workstation.integration.util.FrameworkAccess;
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
        id = "RemoveSecondaryDataAction"
)
@ActionRegistration(
        displayName = "#CTL_RemoveSecondaryDataAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions/Sample", position = 531)
})
@NbBundle.Messages("CTL_RemoveSecondaryDataAction=Remove Secondary Data")
public class RemoveSecondaryDataAction extends BaseContextualPopupAction {

    private static final String WHOLE_AA_REMOVAL_MSG = "Remove/preclude anatomical area of sample";
    private static final String STITCHED_IMG_REMOVAL_MSG = "Remove/preclude Stitched Image";
    private static final String NEURON_SEP_REMOVAL_MSG = "Remove/preclude Neuron Separation(s)";

    private Sample sample;

    @Override
    protected void processContext() {
        this.sample = null;
        if (getNodeContext().isSingleObjectOfType(Sample.class)) {
            this.sample = getNodeContext().getSingleObjectOfType(Sample.class);
            setVisible(true);
            setEnabled(ClientDomainUtils.hasWriteAccess(sample));
        }
        else {
            setEnabledAndVisible(false);
        }
    }

    @Override
    protected List<JComponent> getItems() {

        List<JComponent> items = new ArrayList<>();

        items.add(getPartialSecondaryDataDeletionItem());
        items.add(getStitchedImageDeletionItem());

        /* Removing this feature until such time as this level of flexibility has user demand. */
        if (Utils.SUPPORT_NEURON_SEPARATION_PARTIAL_DELETION_IN_GUI) {
            items.add(getNeuronSeparationDeletionItem());
        }

        return items;
    }

    private JMenuItem getPartialSecondaryDataDeletionItem() {
        JMenuItem rtnVal = new JMenuItem(WHOLE_AA_REMOVAL_MSG);
            rtnVal.addActionListener(ae -> {
                SecondaryDataRemovalDialog dialog = new SecondaryDataRemovalDialog(
                        FrameworkAccess.getMainFrame(),
                        sample,
                        WHOLE_AA_REMOVAL_MSG,
                        Constants.TRIM_DEPTH_WHOLE_AREA_VALUE

                );
                dialog.setVisible(true);
            });
        return rtnVal;
    }

    private JMenuItem getStitchedImageDeletionItem() {
        JMenuItem rtnVal = new JMenuItem( STITCHED_IMG_REMOVAL_MSG);
        rtnVal.addActionListener(ae -> {
            SecondaryDataRemovalDialog dialog = new SecondaryDataRemovalDialog(
                    FrameworkAccess.getMainFrame(),
                    sample,
                    STITCHED_IMG_REMOVAL_MSG,
                    Constants.TRIM_DEPTH_AREA_IMAGE_VALUE
            );
            dialog.setVisible(true);
        });
        return rtnVal;
    }

    private JMenuItem getNeuronSeparationDeletionItem() {
        JMenuItem rtnVal = new JMenuItem(NEURON_SEP_REMOVAL_MSG);
        rtnVal.addActionListener(ae -> {
            SecondaryDataRemovalDialog dialog = new SecondaryDataRemovalDialog(
                    FrameworkAccess.getMainFrame(),
                    sample,
                    NEURON_SEP_REMOVAL_MSG,
                    Constants.TRIM_DEPTH_NEURON_SEPARATION_VALUE
            );
            dialog.setVisible(true);
        });
        return rtnVal;
    }

}