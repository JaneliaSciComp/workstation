package org.janelia.it.workstation.gui.browser.actions;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import org.janelia.it.jacs.model.domain.sample.NeuronSeparation;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.browser.ws.ExternalClient;
import org.janelia.it.workstation.gui.browser.ws.ExternalClientMgr;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.framework.tool_manager.ToolMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Given an entity with a File Path, reveal the path in Finder.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OpenInNeuronAnnotatorAction implements NamedAction {

    private final static Logger log = LoggerFactory.getLogger(OpenInNeuronAnnotatorAction.class);
    
    private NeuronSeparation separation;
    
    public OpenInNeuronAnnotatorAction(NeuronSeparation separation) {
        this.separation = separation;
    }

    @Override
    public String getName() {
        return "View In Neuron Annotator";
    }

    @Override
    public void doAction() {
        try {
            // Check that there is a valid NA instance running
            List<ExternalClient> clients = ExternalClientMgr.getInstance().getExternalClientsByName(ModelMgr.NEURON_ANNOTATOR_CLIENT_NAME);
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

            if (ExternalClientMgr.getInstance().getExternalClientsByName(ModelMgr.NEURON_ANNOTATOR_CLIENT_NAME).isEmpty()) {
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
                ModelMgr.NEURON_ANNOTATOR_CLIENT_NAME);
        ToolMgr.runTool(ToolMgr.TOOL_NA);
        boolean notRunning = true;
        int killCount = 0;
        while (notRunning && killCount < 2) {
            if (SessionMgr.getSessionMgr()
                    .getExternalClientsByName(ModelMgr.NEURON_ANNOTATOR_CLIENT_NAME).isEmpty()) {
                log.debug("Waiting for {} to start.", ModelMgr.NEURON_ANNOTATOR_CLIENT_NAME);
                Thread.sleep(3000);
                killCount++;
            }
            else {
                notRunning = false;
            }
        }
    }
}
