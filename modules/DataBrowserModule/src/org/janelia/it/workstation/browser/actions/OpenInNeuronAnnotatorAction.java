package org.janelia.it.workstation.browser.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.tools.ToolMgr;
import org.janelia.it.workstation.browser.web.FileProxyService;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.it.workstation.browser.ws.ExternalClient;
import org.janelia.it.workstation.browser.ws.ExternalClientMgr;
import org.janelia.model.access.domain.DomainUtils;
import org.janelia.model.access.domain.SampleUtils;
import org.janelia.model.domain.enums.FileType;
import org.janelia.model.domain.sample.NeuronFragment;
import org.janelia.model.domain.sample.NeuronSeparation;
import org.janelia.model.domain.sample.PipelineResult;
import org.janelia.model.domain.sample.Sample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Given an entity with a File Path, reveal the path in Finder.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OpenInNeuronAnnotatorAction extends AbstractAction {

    private final static Logger log = LoggerFactory.getLogger(OpenInNeuronAnnotatorAction.class);

    public static final String NEURON_ANNOTATOR_CLIENT_NAME = "NeuronAnnotator";
    
    private NeuronFragment fragment;
    private NeuronSeparation separation;
    private PipelineResult result;

    public OpenInNeuronAnnotatorAction(NeuronFragment fragment) {
        super("View In Neuron Annotator");
        this.fragment = fragment;
    }

    public OpenInNeuronAnnotatorAction(NeuronSeparation separation) {
        super("View In Neuron Annotator");
        this.separation = separation;
    }

    public OpenInNeuronAnnotatorAction(PipelineResult result) {
        super("View In Neuron Annotator");
        this.result = result;
    }
    
    @Override
    public void actionPerformed(ActionEvent event) {

        ActivityLogHelper.logUserAction("OpenInNeuronAnnotatorAction.doAction", fragment==null?separation:fragment.getSeparationId());

        if (separation!=null) {
            openSeparation();
            return;
        }
        
        if (result!=null) {
            openStack();
            return;  
        }

        SimpleWorker worker = new SimpleWorker() {
            Sample sample;

            @Override
            protected void doStuff() throws Exception {
                if (fragment==null) {
                    throw new IllegalStateException("Both fragment and separation were null");
                }
                sample = (Sample) DomainMgr.getDomainMgr().getModel().getDomainObject(fragment.getSample());
                if (sample != null) {
                    separation = SampleUtils.getNeuronSeparation(sample, fragment);
                }
            }

            @Override
            protected void hadSuccess() {
                if (sample==null) {
                    JOptionPane.showMessageDialog(ConsoleApp.getMainFrame(), "This neuron fragment is orphaned and its sample cannot be loaded.", "Sample data missing", JOptionPane.ERROR_MESSAGE);
                }
                else if (separation==null) {
                    JOptionPane.showMessageDialog(ConsoleApp.getMainFrame(), "This neuron fragment is orphaned and its separation cannot be loaded.", "Neuron separation data missing", JOptionPane.ERROR_MESSAGE);
                }
                else {
                    openSeparation();
                }
            }

            @Override
            protected void hadError(Throwable error) {
                ConsoleApp.handleException(error);
            }
        };

        worker.execute();
    }

    private void openSeparation() {
        if (separation.getParentRun()!=null && separation.getParentRun().getParent().getParent().isSamplePurged()) {
            int result = JOptionPane.showConfirmDialog(ConsoleApp.getMainFrame(), "This sample was previously purged, and may not load correctly in Neuron Annotator. Continue with load?",  "Data missing", JOptionPane.OK_CANCEL_OPTION);
            if (result==JOptionPane.CANCEL_OPTION) {
                return;
            }
        }
        try {
            ensureNAIsRunning();
            log.debug("Requesting view of separation {} in Neuron Annotator: ", separation.getId());
            ExternalClientMgr.getInstance().sendNeuronSeparationRequested(separation);
        }
        catch (Exception e) {
            ConsoleApp.handleException(e);
        }
    }

    private void openStack() {
        try {
            ensureNAIsRunning();
            
            if (DomainUtils.getFilepath(result, FileType.LosslessStack)!=null) {
                log.debug("Requesting view of lossless result {} in Neuron Annotator: ", result.getId());
                ExternalClientMgr.getInstance().sendImageRequested(result, FileType.LosslessStack);
            }
            else if (DomainUtils.getFilepath(result, FileType.VisuallyLosslessStack)!=null) {
                log.debug("Requesting view of lossy result {} in Neuron Annotator: ", result.getId());
                ExternalClientMgr.getInstance().sendImageRequested(result, FileType.VisuallyLosslessStack);
            }
            else {
                JOptionPane.showMessageDialog(ConsoleApp.getMainFrame(),
                    "Result has no associated image stack that can be viewed in Neuron Annotator.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        catch (Exception e) {
            ConsoleApp.handleException(e);
        }
    }
    
    private void ensureNAIsRunning() throws Exception {

        // Check that there is a valid NA instance running
        List<ExternalClient> clients = ExternalClientMgr.getInstance().getExternalClientsByName(NEURON_ANNOTATOR_CLIENT_NAME);

        if (clients.isEmpty()) {
            // If no NA client then try to start one
            startNA();
        }
        else {
            // If NA clients "exist", make sure they are up
            ArrayList<ExternalClient> finalList = new ArrayList<>();
            for (ExternalClient client : clients) {
                boolean connected = client.isConnected();
                if (!connected) {
                    log.debug("Removing client "+client.getName()+" as the heartbeat came back negative.");
                    ExternalClientMgr.getInstance().removeExternalClientByPort(client.getClientPort());
                }
                else {
                    finalList.add(client);
                }
            }
            // If none are up then start one
            if (finalList.isEmpty()) {
                startNA();
            }
        }

        if (ExternalClientMgr.getInstance().getExternalClientsByName(NEURON_ANNOTATOR_CLIENT_NAME).isEmpty()) {
            JOptionPane.showMessageDialog(ConsoleApp.getMainFrame(),
                    "Could not get Neuron Annotator to launch and connect. "
                            + "Please contact support.", "Launch Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
    }

    private void startNA() throws Exception {
        log.debug("Client {} is not running. Starting a new instance.",
                NEURON_ANNOTATOR_CLIENT_NAME);
        ToolMgr.runTool(ToolMgr.TOOL_NA);
        boolean notRunning = true;
        int killCount = 0;
        while (notRunning && killCount < 2) {
            if (ExternalClientMgr.getInstance().getExternalClientsByName(NEURON_ANNOTATOR_CLIENT_NAME).isEmpty()) {
                log.debug("Waiting for {} to start.", NEURON_ANNOTATOR_CLIENT_NAME);
                Thread.sleep(3000);
                killCount++;
            }
            else {
                notRunning = false;
            }
        }
    }
}
