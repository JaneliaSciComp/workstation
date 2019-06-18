package org.janelia.workstation.browser.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;

import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.browser.gui.dialogs.CompressionDialog;
import org.janelia.workstation.common.actions.ViewerContextAction;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.janelia.workstation.integration.spi.domain.ContextualActionUtils;
import org.openide.util.lookup.ServiceProvider;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = ContextualActionBuilder.class, position=520)
public class SampleCompressionBuilder implements ContextualActionBuilder {

    private static SampleCompressionAction action = new SampleCompressionAction();

    @Override
    public boolean isCompatible(Object obj) {
        return obj instanceof Sample;
    }

    @Override
    public Action getAction(Object obj) {
        return action;
    }

    public static class SampleCompressionAction extends ViewerContextAction {

        private List<Sample> samples;

        @Override
        public String getName() {
            return "Change Sample Compression Strategy";
        }

        @Override
        public void setup() {

            this.samples = new ArrayList<>();
            for (Object obj : getViewerContext().getSelectedObjects()) {
                if (obj instanceof Sample) {
                    samples.add((Sample)obj);
                }
            }

            ContextualActionUtils.setVisible(this, false);
            if (!samples.isEmpty()) {
                ContextualActionUtils.setVisible(this, true);
                ContextualActionUtils.setEnabled(this, true);
                for(Sample sample : samples) {
                    if (!ClientDomainUtils.hasWriteAccess(sample)) {
                        ContextualActionUtils.setEnabled(this, false);
                    }
                }
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            CompressionDialog dialog = new CompressionDialog();
            dialog.showForSamples(samples);
        }

    }
}