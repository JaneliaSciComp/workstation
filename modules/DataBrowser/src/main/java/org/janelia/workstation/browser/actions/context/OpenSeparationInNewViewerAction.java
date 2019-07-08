package org.janelia.workstation.browser.actions.context;

import org.janelia.model.domain.SampleUtils;
import org.janelia.model.domain.sample.NeuronFragment;
import org.janelia.model.domain.sample.PipelineResult;
import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.browser.gui.components.SampleResultViewerManager;
import org.janelia.workstation.browser.gui.components.SampleResultViewerTopComponent;
import org.janelia.workstation.browser.gui.components.ViewerUtils;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import javax.swing.JOptionPane;

import java.util.concurrent.Callable;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "Actions",
        id = "OpenSeparationInNewViewerAction"
)
@ActionRegistration(
        displayName = "#CTL_OpenSeparationInNewViewerAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions", position = 60, separatorBefore = 49)
})
@NbBundle.Messages("CTL_OpenSeparationInNewViewerAction=Open Neuron Separation In New Viewer")
public class OpenSeparationInNewViewerAction extends BaseContextualNodeAction {

    private NeuronFragment neuronFragment;

    @Override
    protected void processContext() {
        this.neuronFragment = null;
        if (getNodeContext().isSingleObjectOfType(NeuronFragment.class)) {
            this.neuronFragment = getNodeContext().getSingleObjectOfType(NeuronFragment.class);
            setEnabledAndVisible(true);
        }
        else {
            setEnabledAndVisible(false);
        }
    }

    @Override
    public void performAction() {
        ActivityLogHelper.logUserAction("DomainObjectContentMenu.openSeparationInNewEditorItem", neuronFragment);
        final SampleResultViewerTopComponent viewer = ViewerUtils.createNewViewer(SampleResultViewerManager.getInstance(), "editor3");

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
