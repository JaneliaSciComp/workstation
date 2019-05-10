package org.janelia.workstation.browser.actions;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.SampleUtils;
import org.janelia.model.domain.sample.NeuronFragment;
import org.janelia.model.domain.sample.PipelineResult;
import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.browser.gui.components.SampleResultViewerManager;
import org.janelia.workstation.browser.gui.components.SampleResultViewerTopComponent;
import org.janelia.workstation.browser.gui.components.ViewerUtils;
import org.janelia.workstation.common.gui.util.DomainUIUtils;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.core.actions.ViewerContextReceiver;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.janelia.workstation.integration.spi.domain.ContextualActionUtils;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.util.lookup.ServiceProvider;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.concurrent.Callable;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = ContextualActionBuilder.class, position=60)
public class OpenSeparationInNewViewerBuilder implements ContextualActionBuilder {

    private static final OpenSeparationInNewViewerAction action = new OpenSeparationInNewViewerAction();

    @Override
    public boolean isCompatible(Object obj) {
        return obj instanceof NeuronFragment;
    }

    @Override
    public Action getAction(Object obj) {
        return action;
    }

    private static class OpenSeparationInNewViewerAction extends AbstractAction implements ViewerContextReceiver {

        private DomainObject domainObject;

        OpenSeparationInNewViewerAction() {
            super("Open Neuron Separation In New Viewer");
        }

        @Override
        public void setViewerContext(ViewerContext viewerContext) {
            this.domainObject = DomainUIUtils.getLastSelectedDomainObject(viewerContext);
            ContextualActionUtils.setVisible(this, domainObject!=null && !viewerContext.isMultiple());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ActivityLogHelper.logUserAction("DomainObjectContentMenu.openSeparationInNewEditorItem", domainObject);
            final SampleResultViewerTopComponent viewer = ViewerUtils.createNewViewer(SampleResultViewerManager.getInstance(), "editor3");
            final NeuronFragment neuronFragment = (NeuronFragment)domainObject;

            SimpleWorker worker = new SimpleWorker() {
                private Sample sample;
                private PipelineResult result;

                @Override
                protected void doStuff() throws Exception {
                    sample = DomainMgr.getDomainMgr().getModel().getDomainObject(neuronFragment.getSample());
                    if (sample!=null) {
                        result = SampleUtils.getResultContainingNeuronSeparation(sample, neuronFragment);
                    }
                }

                @Override
                protected void hadSuccess() {
                    if (sample==null) {
                        JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(), "This neuron fragment is orphaned and its sample cannot be loaded.", "Sample data missing", JOptionPane.ERROR_MESSAGE);
                    }
                    else if (result==null) {
                        JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(), "This neuron fragment is orphaned and its separation cannot be loaded.", "Neuron separation data missing", JOptionPane.ERROR_MESSAGE);
                    }
                    else {
                        viewer.requestActive();
                        viewer.loadSampleResult(result, true, new Callable<Void>() {
                            @Override
                            public Void call() throws Exception {
                                // TODO: It would be nice to select the NeuronFragment that the user clicked on to get here, but the required APIs are not curently easily accessible from the outside
                                return null;
                            }
                        });
                    }
                }

                @Override
                protected void hadError(Throwable error) {
                    FrameworkAccess.handleException(error);
                }
            };
            worker.execute();
        }
    }

}
