package org.janelia.it.workstation.gui.framework.session_mgr;

import java.util.*;

/**
 * The SessionModel manages BrowserModels, as well as providing the API
 * for event registeration/deregistration for the BrowserModels.  This
 * class follows the Singleton pattern (Gamma, et al.).  As such, you
 * must get the instance using SessionModel.getSessionModel().
 * <p/>
 * Initially written by: Peter Davies
 */

public class SessionModel extends GenericModel {
    private static SessionModel sessionModel = new SessionModel();
    private Vector browserModels = new Vector(10);
    private static org.janelia.it.workstation.gui.framework.keybind.KeyBindings bindings;
    private List<org.janelia.it.workstation.ws.ExternalClient> externalClients = new ArrayList<org.janelia.it.workstation.ws.ExternalClient>();
    private int portCounter = 30020;

    private SessionModel() {
        super();
        browserModels = new Vector(10);
        // Load Key Bindings
        bindings = new org.janelia.it.workstation.gui.framework.keybind.KeyBindings();
    }  //Singleton pattern enforcement --PED 5/13

    static SessionModel getSessionModel() {
        return sessionModel;
    } //Only the SessionManager should have direct access.

    public BrowserModel addBrowserModel() {
        BrowserModel browserModel = new BrowserModel();
        browserModels.addElement(browserModel);
        fireBrowserAdded(browserModel);
        return browserModel;
    }

    void addBrowserModel(BrowserModel browserModel) {
        browserModels.addElement(browserModel);
        fireBrowserAdded(browserModel);
    }

    /**
     * Exit the application if the last browserModel is removed
     */

    public void removeBrowserModel(BrowserModel browserModel) {
        browserModels.removeElement(browserModel);
        browserModel.dispose();
        fireBrowserRemoved(browserModel);
        if (browserModels.isEmpty()) SessionMgr.getSessionMgr().systemExit();
    }

    /**
     * Exit the application with full notification
     */
    public void removeAllBrowserModels() {
        BrowserModel browserModel;
        for (Enumeration e = browserModels.elements(); e.hasMoreElements(); ) {
            browserModel = (BrowserModel) e.nextElement();
            browserModel.dispose();
            fireBrowserRemoved(browserModel);
        }
    }

    public void addSessionListener(org.janelia.it.workstation.gui.framework.session_mgr.SessionModelListener sessionModelListener) {
        for (Enumeration e = browserModels.elements(); e.hasMoreElements(); )
            sessionModelListener.browserAdded((BrowserModel) e.nextElement());
        if (!modelListeners.contains(sessionModelListener)) modelListeners.add(sessionModelListener);
    }

    public void removeSessionListener(org.janelia.it.workstation.gui.framework.session_mgr.SessionModelListener sessionModelListener) {
        modelListeners.remove(sessionModelListener);
    }

    public int getNumberOfBrowserModels() {
        return this.browserModels.size();
    }

    /**
     * Since there can be more than one external client of a given tool add them all as listeners and the
     * instantiation of ExternalClients will add the unique timestamp id
     * @param newClientName the external tool to add
     */
    public int addExternalClient(String newClientName) {
        org.janelia.it.workstation.ws.ExternalClient newClient = new org.janelia.it.workstation.ws.ExternalClient(++portCounter,newClientName);
        externalClients.add(newClient);
        return newClient.getClientPort();
    }

    public List<org.janelia.it.workstation.ws.ExternalClient> getExternalClientsByName(String clientName){
        List<org.janelia.it.workstation.ws.ExternalClient> returnList = new ArrayList<org.janelia.it.workstation.ws.ExternalClient>();
        for (org.janelia.it.workstation.ws.ExternalClient externalClient : externalClients) {
            if (externalClient.getName().equals(clientName)) { returnList.add(externalClient); }
        }
        return returnList;
    }

    public org.janelia.it.workstation.ws.ExternalClient getExternalClientByPort(int targetPort) {
        for (org.janelia.it.workstation.ws.ExternalClient externalClient : externalClients) {
            // There can be only one - client-to-port that is...
            if (externalClient.getClientPort()==targetPort) {
                return externalClient;
            }
        }
        return null;
    }

    public void removeExternalClientByPort(int targetPort){
        org.janelia.it.workstation.ws.ExternalClient targetClient=null;
        for (org.janelia.it.workstation.ws.ExternalClient externalClient : externalClients) {
            if (externalClient.getClientPort()==targetPort){
                targetClient = externalClient;
                break;
            }
        }
        if (null!=targetClient) { externalClients.remove(targetClient); }
    }

    public List<org.janelia.it.workstation.ws.ExternalClient> getExternalClients() {
        return externalClients;
    }
    
    public void sendMessageToExternalClients(String operationName, Map<String,Object> parameters) {
        for (org.janelia.it.workstation.ws.ExternalClient externalClient : externalClients) {
        	try {
        		externalClient.sendMessage(operationName, parameters);
        	}
        	catch (Exception e) {
        		e.printStackTrace();
        	}
        }
    }

    public void systemWillExit() {
        removeAllBrowserModels();
        fireSystemExit();
    }

//  void loadProgressMeterStateChange(boolean on) {
//     fireLoadProgressStateChange(on);
//  }

    private void fireBrowserRemoved(BrowserModel browserModel) {
        for (GenericModelListener modelListener : modelListeners)
            ((org.janelia.it.workstation.gui.framework.session_mgr.SessionModelListener) modelListener).browserRemoved(browserModel);
    }

    private void fireBrowserAdded(BrowserModel browserModel) {
        for (GenericModelListener modelListener : modelListeners)
            ((org.janelia.it.workstation.gui.framework.session_mgr.SessionModelListener) modelListener).browserAdded(browserModel);
    }

    private void fireSystemExit() {
        for (GenericModelListener modelListener : modelListeners)
            ((org.janelia.it.workstation.gui.framework.session_mgr.SessionModelListener) modelListener).sessionWillExit();
    }

    public static org.janelia.it.workstation.gui.framework.keybind.KeyBindings getKeyBindings() {
        return bindings;
    }

}
