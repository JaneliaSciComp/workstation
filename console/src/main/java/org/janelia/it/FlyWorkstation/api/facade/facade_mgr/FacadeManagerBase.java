package org.janelia.it.FlyWorkstation.api.facade.facade_mgr;

import org.janelia.it.FlyWorkstation.api.facade.abstract_facade.*;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 7/22/11
 * Time: 2:59 PM
 */
public abstract class FacadeManagerBase {

    public static final ConnectionStatus CONNECTION_STATUS_OK = new ConnectionStatus("Connection OK", false);
    public static final ConnectionStatus CONNECTION_STATUS_BAD_CREDENTIALS = new ConnectionStatus("Invalid Username/Password", true);
    public static final ConnectionStatus CONNECTION_STATUS_NO_CREDENTIALS = new ConnectionStatus("No defined Username/Password", false);
    public static final ConnectionStatus CONNECTION_STATUS_NO_DEFINED_INFORMATION_SERVICE = new ConnectionStatus("No Location for the Information Service was defined", false);
    public static final ConnectionStatus CONNECTION_STATUS_CANNOT_CONNECT = new ConnectionStatus("Cannot connect to Information Service", true);

    public abstract ControlledVocabService getControlledVocabService() throws Exception;

    public abstract String getDataSourceSelectorClass(); //note class must implement DataSourceSelector

    /**
     * This method will be called to initialize the FacadeManager instance.
     */
    public ConnectionStatus initiateConnection() {
        return CONNECTION_STATUS_OK;
    }

    public void prepareForSystemExit() {
    } //Override to receive system exit notification

    public boolean canAddMoreDataSources() {
        return false;
    } //indicates the ability for the protocol to accept more than 1 datasource

    public abstract String getServerName();

    public abstract EntityFacade getFacade(String entityTypeName) throws Exception;

    /**
     * Return the open datasources for the facade. toString will be used to display the sources
     */
    public abstract Object[] getOpenDataSources();

    //     private static final int OBJECT_BD_GRTR_THAN_PARAM = 1;  // As used in BigDecimal.compareTo

    public abstract OntologyFacade getOntologyFacade();

    public abstract EntityFacade getEntityFacade();

    public abstract AnnotationFacade getAnnotationFacade();

    public abstract SolrFacade getSolrFacade();
    
    public abstract ComputeFacade getComputeFacade();
}

