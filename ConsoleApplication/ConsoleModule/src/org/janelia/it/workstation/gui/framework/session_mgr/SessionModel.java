package org.janelia.it.workstation.gui.framework.session_mgr;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.janelia.it.workstation.ws.ExternalClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The SessionModel manages BrowserModels, as well as providing the API
 * for event registeration/deregistration for the BrowserModels.  This
 * class follows the Singleton pattern (Gamma, et al.).  As such, you
 * must get the instance using SessionModel.getSessionModel().
 * <p/>
 * Initially written by: Peter Davies
 */

public class SessionModel extends GenericModel {

    private static final Logger log = LoggerFactory.getLogger(SessionModel.class);
    
    private static SessionModel sessionModel = new SessionModel();
    private List<BrowserModel> browserModels = new ArrayList<>();
    private List<ExternalClient> externalClients = new ArrayList<>();
    private int portOffset = 0;
    private int portCounter = 1;

    private SessionModel() {
        super();
        browserModels = new ArrayList<>();
        // Load Key Bindings
    }  //Singleton pattern enforcement --PED 5/13

    static SessionModel getSessionModel() {
        return sessionModel;
    } //Only the SessionManager should have direct access.

    public BrowserModel addBrowserModel() {
        BrowserModel browserModel = new BrowserModel();
        browserModels.add(browserModel);
        fireBrowserAdded(browserModel);
        return browserModel;
    }
    
    public void setPortOffset(int portOffset) {
    	this.portOffset = portOffset;
    }

//    void addBrowserModel(BrowserModel browserModel) {
//        browserModels.add(browserModel);
//        fireBrowserAdded(browserModel);
//    }
//
    /**
     * Exit the application if the last browserModel is removed
     */

//    public void removeBrowserModel(BrowserModel browserModel) {
//        browserModels.remove(browserModel);
//        browserModel.dispose();
//        fireBrowserRemoved(browserModel);
//        if (browserModels.isEmpty()) SessionMgr.getSessionMgr().systemExit();
//    }

    /**
     * Exit the application with full notification
     */
    public void removeAllBrowserModels() {
        for (BrowserModel browserModel : browserModels) {
            browserModel.dispose();
            fireBrowserRemoved(browserModel);
        }
    }

    public void addSessionListener(SessionModelListener sessionModelListener) {
        for (BrowserModel browserModel : browserModels) {
            sessionModelListener.browserAdded(browserModel);
        }
        if (!modelListeners.contains(sessionModelListener)) modelListeners.add(sessionModelListener);
    }

    public void removeSessionListener(SessionModelListener sessionModelListener) {
        modelListeners.remove(sessionModelListener);
    }

    /**
     * Since there can be more than one external client of a given tool add them all as listeners and the
     * instantiation of ExternalClients will add the unique timestamp id
     * @param newClientName the external tool to add
     */
    public int addExternalClient(String newClientName) {
        ExternalClient newClient = new ExternalClient(portOffset+portCounter, newClientName);
        externalClients.add(newClient);
        this.portCounter+=1;
        return newClient.getClientPort();
    }

    public List<ExternalClient> getExternalClientsByName(String clientName){
        List<ExternalClient> returnList = new ArrayList<>();
        for (ExternalClient externalClient : externalClients) {
            if (externalClient.getName().equals(clientName)) { returnList.add(externalClient); }
        }
        return returnList;
    }

    public ExternalClient getExternalClientByPort(int targetPort) {
        for (ExternalClient externalClient : externalClients) {
            // There can be only one - client-to-port that is...
            if (externalClient.getClientPort()==targetPort) {
                return externalClient;
            }
        }
        return null;
    }

    public void removeExternalClientByPort(int targetPort) {
        ExternalClient targetClient = null;
        for (ExternalClient externalClient : externalClients) {
            if (externalClient.getClientPort() == targetPort) {
                targetClient = externalClient;
                break;
            }
        }
        if (null != targetClient) {
            externalClients.remove(targetClient);
        }
    }

    public void sendMessageToExternalClients(String operationName, Map<String, Object> parameters) {
        for (ExternalClient externalClient : externalClients) {
            try {
                externalClient.sendMessage(operationName, parameters);
            } 
            catch (Exception e) {
                log.error("Error sending message to external clients: " + operationName, e);
            }
        }
    }

    public void systemWillExit() {
        removeAllBrowserModels();
        fireSystemExit();
    }

    private void fireBrowserRemoved(BrowserModel browserModel) {
        for (GenericModelListener modelListener : modelListeners)
            ((SessionModelListener) modelListener).browserRemoved(browserModel);
    }

    private void fireBrowserAdded(BrowserModel browserModel) {
        for (GenericModelListener modelListener : modelListeners)
            ((SessionModelListener) modelListener).browserAdded(browserModel);
    }

    private void fireSystemExit() {
        for (GenericModelListener modelListener : modelListeners)
            ((SessionModelListener) modelListener).sessionWillExit();
    }

}
