package org.janelia.it.workstation.gui.browser.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.sample.NeuronSeparation;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.support.SampleUtils;
import org.janelia.it.workstation.gui.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.workers.SimpleWorker;
import org.janelia.it.workstation.gui.browser.ws.ExternalClient;
import org.janelia.it.workstation.gui.browser.ws.ExternalClientMgr;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.browser.tools.ToolMgr;
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

    public OpenInNeuronAnnotatorAction(NeuronFragment fragment) {
        super("View In Neuron Annotator");
        this.fragment = fragment;
    }

    public OpenInNeuronAnnotatorAction(NeuronSeparation separation) {
        super("View In Neuron Annotator");
        this.separation = separation;
    }
    
    @Override
    public void actionPerformed(ActionEvent event) {

        ActivityLogHelper.logUserAction("OpenInNeuronAnnotatorAction.doAction", fragment==null?separation:fragment.getSeparationId());

        if (separation!=null) {
            open();
            return;
        }

        SimpleWorker worker = new SimpleWorker() {
            Sample sample;

            @Override
            protected void doStuff() throws Exception {
                sample = (Sample) DomainMgr.getDomainMgr().getModel().getDomainObject(fragment.getSample());
                if (sample != null) {
                    separation = SampleUtils.getNeuronSeparation(sample, fragment);
                }
            }

            @Override
            protected void hadSuccess() {
                if (sample==null) {
                    JOptionPane.showMessageDialog(SessionMgr.getMainFrame(), "This neuron fragment is orphaned and its sample cannot be loaded.", "Sample data missing", JOptionPane.ERROR_MESSAGE);
                }
                else if (separation==null) {
                    JOptionPane.showMessageDialog(SessionMgr.getMainFrame(), "This neuron fragment is orphaned and its separation cannot be loaded.", "Neuron separation data missing", JOptionPane.ERROR_MESSAGE);
                }
                else {
                    open();
                }
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }
        };

        worker.execute();
    }

    private void open() {

        try {
            // Check that there is a valid NA instance running
            List<ExternalClient> clients = ExternalClientMgr.getInstance().getExternalClientsByName(NEURON_ANNOTATOR_CLIENT_NAME);
            // If no NA client then try to start one
            if (clients.isEmpty()) {
                startNA();
            }
            // If NA clients "exist", make sure they are up
            else {
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
                JOptionPane.showMessageDialog(SessionMgr.getMainFrame(),
                        "Could not get Neuron Annotator to launch and connect. "
                                + "Please contact support.", "Launch ERROR", JOptionPane.ERROR_MESSAGE);
                return;
            }

            log.debug("Requesting entity view in Neuron Annotator: " + separation.getId());
            ExternalClientMgr.getInstance().sendNeuronSeparationRequested(separation);
        }
        catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
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
