package org.janelia.workstation.browser.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.browser.nb_action.ApplyPublishingNamesAction;
import org.janelia.workstation.common.actions.ViewerContextAction;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.core.actions.ViewerContextReceiver;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.janelia.workstation.integration.spi.domain.ContextualActionUtils;
import org.openide.util.lookup.ServiceProvider;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = ContextualActionBuilder.class, position=540)
public class ApplyPublishingNamesBuilder implements ContextualActionBuilder {

    private static ApplyPublishingNamesHarness action = new ApplyPublishingNamesHarness();

    @Override
    public boolean isCompatible(Object obj) {
        return obj instanceof Sample;
    }

    @Override
    public Action getAction(Object obj) {
        return action;
    }

    public static class ApplyPublishingNamesHarness extends ViewerContextAction {

        private List<Sample> samples;
        private ApplyPublishingNamesAction innerAction;

        @Override
        public String getName() {
            return ContextualActionUtils.getName(innerAction);
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
            if (samples.size()==viewerContext.getSelectedObjects().size()) {
                ContextualActionUtils.setVisible(this, true);
                ContextualActionUtils.setEnabled(this, true);
                for(Sample sample : samples) {
                    if (!ClientDomainUtils.hasWriteAccess(sample)) {
                        ContextualActionUtils.setEnabled(this, false);
                    }
                }
            }

            this.innerAction = new ApplyPublishingNamesAction(samples);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (innerAction != null) {
                innerAction.actionPerformed(e);
            }
        }

    }
}