package org.janelia.workstation.browser.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.janelia.it.jacs.shared.utils.Constants;
import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.browser.gui.dialogs.SecondaryDataRemovalDialog;
import org.janelia.workstation.common.actions.ViewerContextAction;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.util.Utils;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.janelia.workstation.integration.spi.domain.ContextualActionUtils;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.util.lookup.ServiceProvider;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = ContextualActionBuilder.class, position=531)
public class PartialSecondaryDataDeletionBuilder implements ContextualActionBuilder {

    private static final String WHOLE_AA_REMOVAL_MSG = "Remove/preclude anatomical area of sample";
    private static final String STITCHED_IMG_REMOVAL_MSG = "Remove/preclude Stitched Image";
    private static final String NEURON_SEP_REMOVAL_MSG = "Remove/preclude Neuron Separation(s)";

    private static PartialSecondaryDataDeletionAction action = new PartialSecondaryDataDeletionAction();

    @Override
    public boolean isCompatible(Object obj) {
        return obj instanceof Sample;
    }

    @Override
    public Action getAction(Object obj) {
        return action;
    }

    public static class PartialSecondaryDataDeletionAction extends ViewerContextAction {

        private List<Sample> samples;

        @Override
        public String getName() {
            return "Remove Secondary Data";
        }

        @Override
        public void setup() {
            ViewerContext viewerContext = getViewerContext();

            this.samples = new ArrayList<>();
            for (Object obj : viewerContext.getSelectedObjects()) {
                if (obj instanceof Sample) {
                    samples.add((Sample)obj);
                }
            }

            ContextualActionUtils.setVisible(this, false);
            if (samples.size()==1) {
                ContextualActionUtils.setVisible(this, true);
                ContextualActionUtils.setEnabled(this, true);
                for (Sample sample : samples) {
                    if (!ClientDomainUtils.hasWriteAccess(sample)) {
                        ContextualActionUtils.setEnabled(this, false);
                    }
                }
            }
        }

        @Override
        public JMenuItem getPopupPresenter() {

            JMenu secondaryDeletionMenu = new JMenu(getName());

            JMenuItem itm = getPartialSecondaryDataDeletionItem(samples);
            if (itm != null) {
                secondaryDeletionMenu.add(itm);
            }

            itm = getStitchedImageDeletionItem(samples);
            if (itm != null) {
                secondaryDeletionMenu.add(itm);
            }

            /* Removing this feature until such time as this level of flexibility has user demand. */
            if (Utils.SUPPORT_NEURON_SEPARATION_PARTIAL_DELETION_IN_GUI) {
                itm = getNeuronSeparationDeletionItem();
                if (itm != null) {
                    secondaryDeletionMenu.add(itm);
                }
            }

            if (secondaryDeletionMenu.getItemCount() > 0) {
                for(Sample sample : samples) {
                    if (!ClientDomainUtils.hasWriteAccess(sample)) {
                        secondaryDeletionMenu.setEnabled(false);
                        break;
                    }
                }
                return secondaryDeletionMenu;
            }
            return null;
        }

        private JMenuItem getPartialSecondaryDataDeletionItem(List<Sample> samples) {
            JMenuItem rtnVal = null;
            if (samples.size() == 1) {
                final Sample sample = samples.get(0);
                rtnVal = new JMenuItem(WHOLE_AA_REMOVAL_MSG);
                rtnVal.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        SecondaryDataRemovalDialog dialog = new SecondaryDataRemovalDialog(
                                FrameworkAccess.getMainFrame(),
                                sample,
                                WHOLE_AA_REMOVAL_MSG,
                                Constants.TRIM_DEPTH_WHOLE_AREA_VALUE

                        );
                        dialog.setVisible(true);
                    }
                });
            }
            return rtnVal;
        }

        private JMenuItem getStitchedImageDeletionItem(List<Sample> samples) {
            JMenuItem rtnVal = null;
            if (samples.size() == 1) {
                final Sample sample = samples.get(0);
                rtnVal = new JMenuItem( STITCHED_IMG_REMOVAL_MSG);
                rtnVal.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        SecondaryDataRemovalDialog dialog = new SecondaryDataRemovalDialog(
                                FrameworkAccess.getMainFrame(),
                                sample,
                                STITCHED_IMG_REMOVAL_MSG,
                                Constants.TRIM_DEPTH_AREA_IMAGE_VALUE
                        );
                        dialog.setVisible(true);
                    }
                });
            }
            return rtnVal;
        }

        private JMenuItem getNeuronSeparationDeletionItem() {
            JMenuItem rtnVal = null;
            if (samples.size() == 1) {
                final Sample sample = (Sample)samples.get(0);
                rtnVal = new JMenuItem(NEURON_SEP_REMOVAL_MSG);
                rtnVal.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        SecondaryDataRemovalDialog dialog = new SecondaryDataRemovalDialog(
                                FrameworkAccess.getMainFrame(),
                                sample,
                                NEURON_SEP_REMOVAL_MSG,
                                Constants.TRIM_DEPTH_NEURON_SEPARATION_VALUE
                        );
                        dialog.setVisible(true);
                    }
                });
            }
            return rtnVal;
        }

    }
}