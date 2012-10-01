package org.janelia.it.FlyWorkstation.api.facade.concrete_facade.ejb;


import java.util.HashMap;
import java.util.Map;

import org.janelia.it.FlyWorkstation.api.facade.abstract_facade.*;
import org.janelia.it.FlyWorkstation.api.facade.facade_mgr.ConnectionStatus;
import org.janelia.it.FlyWorkstation.api.facade.facade_mgr.FacadeManagerBase;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.LoginProperties;
import org.janelia.it.FlyWorkstation.shared.util.PropertyConfigurator;
import org.janelia.it.jacs.model.entity.EntityConstants;

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
    private EntityFacade entityFacade;
    private OntologyFacade ontologyFacade;
    private AnnotationFacade annotationFacade;
    private SolrFacade solrFacade;
    private ComputeFacade computeFacade;

    public EJBFacadeManager() {
    } //only instantiated by FacadeManager.

    public Object[] getOpenDataSources() {
        return new Object[]{EJBFactory.getAppServerName()};
    }

    @Override
    public OntologyFacade getOntologyFacade() {
        if (ontologyFacade == null) {
            ontologyFacade = new EJBOntologyFacade();
        }
        return ontologyFacade;
    }

    @Override
    public EntityFacade getEntityFacade() {
        if (entityFacade == null) {
            entityFacade = new EJBEntityFacade();
        }
        return entityFacade;
    }

    @Override
    public AnnotationFacade getAnnotationFacade() {
        if (annotationFacade == null) {
            annotationFacade = new EJBAnnotationFacade();
        }
        return annotationFacade;
    }
    
    @Override
    public SolrFacade getSolrFacade() {
        if (solrFacade == null) {
        	solrFacade = new EJBSolrFacade();
        }
        return solrFacade;
    }

    @Override
    public ComputeFacade getComputeFacade() {
        if (computeFacade == null) {
            computeFacade = new EJBComputeFacade();
        }
        return computeFacade;
    }

    /**
     * This method will be called if the previous one returns true.
     */
    public ConnectionStatus initiateConnection() {
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
            return new ConnectionStatus(ex.getMessage(), true);
        }

        return CONNECTION_STATUS_OK;
    }

    @Override
    public ControlledVocabService getControlledVocabService() throws Exception {
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

    public EntityFacade getFacade(String entityTypeName) throws Exception {
        if (EntityConstants.TYPE_ONTOLOGY_ROOT.equals(entityTypeName) || EntityConstants.TYPE_ONTOLOGY_ELEMENT.equals(entityTypeName)) {
            return getOntologyFacade();
        }
        else if (EntityConstants.TYPE_ANNOTATION.equals(entityTypeName)) {
            return getAnnotationFacade();
        }
        else {
            return getEntityFacade();
        }
    }

    private String getUsername() {
        if (username != null) {
            return username;
        }

        username = PropertyConfigurator.getProperties().getProperty(LoginProperties.SERVER_LOGIN_NAME);

        return username;
    }

    private String getPassword() {
        if (password != null) {
            return password;
        }

        password = PropertyConfigurator.getProperties().getProperty(LoginProperties.SERVER_LOGIN_PASSWORD);

        return password;
    }

}