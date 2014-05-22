package org.janelia.it.workstation.api.facade.concrete_facade.ejb;


import java.util.HashMap;
import java.util.Map;

import org.janelia.it.workstation.api.facade.facade_mgr.FacadeManagerBase;
import org.janelia.it.workstation.shared.util.PropertyConfigurator;

public class EJBFacadeManager extends FacadeManagerBase {
    private static Class[] createMethodArgumentsClass = new Class[0];
    private static Object[] createMethodArgumentsList = new Object[0];

    private boolean isDataSourceSet = false;
    private Map remoteInterfacePools = new HashMap();
    private Map adapters = new HashMap();
    //the default maximum number of interfaces the client will allocate
    private int defaultMaxInterfaces = 10;
    //the default minimum number of interfaces the client will allocate
    private int defaultMinInterfaces = 1;

    private String username;
    private String password;

    // Facades
    private org.janelia.it.workstation.api.facade.abstract_facade.EntityFacade entityFacade;
    private org.janelia.it.workstation.api.facade.abstract_facade.OntologyFacade ontologyFacade;
    private org.janelia.it.workstation.api.facade.abstract_facade.AnnotationFacade annotationFacade;
    private org.janelia.it.workstation.api.facade.abstract_facade.SolrFacade solrFacade;
    private org.janelia.it.workstation.api.facade.abstract_facade.ComputeFacade computeFacade;

    public EJBFacadeManager() {
    } //only instantiated by FacadeManager.

    public Object[] getOpenDataSources() {
        return new Object[]{EJBFactory.getAppServerName()};
    }

    @Override
    public org.janelia.it.workstation.api.facade.abstract_facade.OntologyFacade getOntologyFacade() {
        if (ontologyFacade == null) {
            ontologyFacade = new EJBOntologyFacade();
        }
        return ontologyFacade;
    }

    @Override
    public org.janelia.it.workstation.api.facade.abstract_facade.EntityFacade getEntityFacade() {
        if (entityFacade == null) {
            entityFacade = new org.janelia.it.workstation.api.facade.concrete_facade.ejb.EJBEntityFacade();
        }
        return entityFacade;
    }

    @Override
    public org.janelia.it.workstation.api.facade.abstract_facade.AnnotationFacade getAnnotationFacade() {
        if (annotationFacade == null) {
            annotationFacade = new org.janelia.it.workstation.api.facade.concrete_facade.ejb.EJBAnnotationFacade();
        }
        return annotationFacade;
    }
    
    @Override
    public org.janelia.it.workstation.api.facade.abstract_facade.SolrFacade getSolrFacade() {
        if (solrFacade == null) {
        	solrFacade = new org.janelia.it.workstation.api.facade.concrete_facade.ejb.EJBSolrFacade();
        }
        return solrFacade;
    }

    @Override
    public org.janelia.it.workstation.api.facade.abstract_facade.ComputeFacade getComputeFacade() {
        if (computeFacade == null) {
            computeFacade = new org.janelia.it.workstation.api.facade.concrete_facade.ejb.EJBComputeFacade();
        }
        return computeFacade;
    }

    /**
     * This method will be called if the previous one returns true.
     */
    public org.janelia.it.workstation.api.facade.facade_mgr.ConnectionStatus initiateConnection() {
        try {
            EJBFactory.getRemoteAnnotationBean();
            // KLUDGE!
            // This call is here in order to force authentication to occur
            // at this point. Retrieving an InitialContext does not
            // cause authentication to occur. Performing a lookup does!!!
        }
//        catch (AuthenticationException aEx) {
//            if ((getUsername() == null) || getUsername().equals("")) {
//                return CONNECTION_STATUS_NO_CREDENTIALS;
//            }
//            return CONNECTION_STATUS_BAD_CREDENTIALS;
//        }
        catch (SecurityException secEx) {
            if ((getUsername() == null) || getUsername().equals("")) {
                return CONNECTION_STATUS_NO_CREDENTIALS;
            }
            return CONNECTION_STATUS_BAD_CREDENTIALS;
        }
        catch (Exception ex) {
            return new org.janelia.it.workstation.api.facade.facade_mgr.ConnectionStatus(ex.getMessage(), true);
        }

        return CONNECTION_STATUS_OK;
    }

    @Override
    public org.janelia.it.workstation.api.facade.abstract_facade.ControlledVocabService getControlledVocabService() throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getDataSourceSelectorClass() {
        return "client.gui.other.data_source_selectors.GenomeVersionSelector";
    } //note class must implement DataSourceSelector

    public boolean canAddMoreDataSources() {
        return !isDataSourceSet;
    }

    @Override
    public String getServerName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    private String getUsername() {
        if (username != null) {
            return username;
        }

        username = PropertyConfigurator.getProperties().getProperty(org.janelia.it.workstation.gui.framework.session_mgr.LoginProperties.SERVER_LOGIN_NAME);

        return username;
    }

    private String getPassword() {
        if (password != null) {
            return password;
        }

        password = PropertyConfigurator.getProperties().getProperty(org.janelia.it.workstation.gui.framework.session_mgr.LoginProperties.SERVER_LOGIN_PASSWORD);

        return password;
    }

}