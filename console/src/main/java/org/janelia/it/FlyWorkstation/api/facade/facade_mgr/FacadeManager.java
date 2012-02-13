package org.janelia.it.FlyWorkstation.api.facade.facade_mgr;

import org.janelia.it.FlyWorkstation.api.facade.concrete_facade.aggregate.AggregateFacadeManager;
import org.janelia.it.FlyWorkstation.api.facade.roles.ExceptionHandler;
import org.janelia.it.FlyWorkstation.api.stub.data.NoDataException;
import org.janelia.it.FlyWorkstation.gui.util.PathTranslator;

import java.util.*;

public class FacadeManager {

    static Hashtable concreteFacadesClasses = new Hashtable();
    static Hashtable concreteFacadesProtocols = new Hashtable();
    static Hashtable concreteFacades = new Hashtable();
    static Hashtable displayNames = new Hashtable();
    static String protocolString;
    static ArrayList inUseProtocols = new ArrayList();
    static List availableProtocols = new Vector();
    static List protocolListeners;
    static boolean useAggregate;
    static final String AGGREGATE_PROTOCOL = "aggregate";
    static final String EJB_PROTOCOL = "ejb";
    static private Vector exceptionHandlers;
//  static private FacadeManagerBase assemblyFacade;
//  static private Set assemblyProtocols= new HashSet();

    static {
//        try {
//            protocolString = ConsoleProperties.getString("console.Protocol");
//        }
//        catch (Exception ex) {
//        }
//        if (protocolString == null) {
//            System.err.println("Protocol has not been set.  Protocol must be a system property " + "passed from the command line with -DProtocol=<protocolName>");
//            System.exit(1);
//        }
//        System.out.println("protocol = " + protocolString);
//        try {
//            FacadeManager.setProtocol(protocolString);
//        }
//        catch (Exception ex) {
//            System.out.println("FacadeManager:: Error!! - Protocol set has not been added to the system");
//            System.exit(1);
//        }
        registerFacade(AGGREGATE_PROTOCOL, AggregateFacadeManager.class);
    }

    private FacadeManager() {
    } //no one can instanciate me

    /**
     * First protocol will be used if it is the only one registered.  Otherwise, the aggregate will be
     * used.
     */
    static public void registerFacade(String protocol, Class facadeClass) {
        if (concreteFacadesClasses.containsKey(protocol)) return;
        //Set the protocolString to the first non-AGGREGATE string passed.
        //If a second non-Aggregate String is passed, set the protocol to
        //the aggregate.  --PED 4/27/01
        if (!protocol.equals(AGGREGATE_PROTOCOL)) {
            if (protocolString != null && !protocolString.equals(AGGREGATE_PROTOCOL)) {
                protocolString = AGGREGATE_PROTOCOL;
            }
            else if (protocolString == null) protocolString = protocol;
        }
        // System.out.println("Registering class " + facadeClass + " for protocoll " + protocol);
        concreteFacadesClasses.put(protocol, facadeClass); //Don't put in display name
        // This maintains the reverse for the data source selectors.
        concreteFacadesProtocols.put(facadeClass, protocol);
    }


    static public void registerFacade(String protocol, Class facadeClass, String displayName) {
        registerFacade(protocol, facadeClass);
        displayNames.put(protocol, displayName);
    }

    static private void setProtocol(String protocol) throws Exception {
        String[] protocols = parseProtocol(protocol);
        for (String protocol1 : protocols) {
            availableProtocols.add(protocol1);
        }
        if (protocols.length > 1) useAggregate = true;
    }

//  static public GenomeVersionInfo getGenomeVersionInfo(int genomeVersionId){
//     return (GenomeVersionInfo) genomeVersionIdToInfo.get(new Integer(genomeVersionId));
//  }
//
//  /**
//   * @return null if unknown
//   */
//  static public GenomeVersionInfo getGenomeVersionWithWorkspace(){
//     return (GenomeVersionInfo) genomeVersionIdToInfo.get(new Integer(genomeVersionWithWorkspaceId));
//  }
//
//  static public void setGenomeVersionWithWorkSpaceId(int genomeVersionId){
//     genomeVersionWithWorkspaceId=genomeVersionId;
//  }
//
//  static public void addAvailableGenomeVersionInfo(GenomeVersionInfo info) {
//    genomeVersionIdToInfo.put(new Integer(info.getGenomeVersionId()), info);
//  }

    static public void resetFacadeManager() {
        String[] protocols = (String[]) inUseProtocols.toArray(new String[0]);
        for (String protocol : protocols) {
            if (protocol != AGGREGATE_PROTOCOL) removeProtocolFromUseList(protocol);
        }
//     removeProtocolFromUseList(AGGREGATE_PROTOCOL);
    }

    static private String[] parseProtocol(String protocolString) {
        Vector protocols = new Vector();
        while (protocolString.indexOf("+") != -1) {
            protocols.add(new String(protocolString.substring(0, protocolString.indexOf("+"))));
            protocolString = protocolString.substring(protocolString.indexOf("+") + 1);
        }
        protocols.add(protocolString);
        return (String[]) protocols.toArray(new String[0]);
    }

    //todo should be removed as soon as XML works with new dataSourceSelector
    static public String getProtocol() {
        if (useAggregate) return AGGREGATE_PROTOCOL;
        return protocolString;
    }

    //
    static public boolean isProtocolRegistered(String protocol) {
        return concreteFacadesClasses.keySet().contains(protocol);
    }

    static public boolean isProtocolInUse(String protocol) {
        return inUseProtocols.contains(protocol);
    }

    /**
     * This method is for the data source selectors and allows them to get the protocol string associated with
     * a given facade.  This string is used when the protocol needs to be added to the in use list.
     */
    static public String getProtocolForFacade(Class facadeClass) {
        return (String) concreteFacadesProtocols.get(facadeClass);
    }

    static public FacadeManagerBase getFacadeManager() {
        try {
            FacadeManagerBase concreteFacade = (FacadeManagerBase) concreteFacades.get(getProtocol());
            if (concreteFacade != null) return concreteFacade;
            concreteFacade = (FacadeManagerBase) ((Class) concreteFacadesClasses.get(getProtocol())).newInstance();
            concreteFacades.put(getProtocol(), concreteFacade);
            return concreteFacade;
        }
        catch (Exception e) {
            handleException(e);
        }
        return null;
    }

    static public FacadeManagerBase getFacadeManager(String protocol) {
        try {
            FacadeManagerBase concreteFacade = (FacadeManagerBase) concreteFacades.get(protocol);
            if (concreteFacade != null) return concreteFacade;
            else {
                concreteFacade = (FacadeManagerBase) ((Class) concreteFacadesClasses.get(protocol)).newInstance();
                concreteFacades.put(protocol, concreteFacade);
                return concreteFacade;
            }
        }
        catch (Exception e) {
            handleException(e);
        }
        return null;
    }


    static public ConnectionStatus addProtocolToUseList(String protocol) {
        if (inUseProtocols.contains(protocol)) {
            return FacadeManagerBase.CONNECTION_STATUS_OK;
        }
        inUseProtocols.add(protocol);
        FacadeManagerBase concreteFacade = (FacadeManagerBase) concreteFacades.get(protocol);
        try {
            // If this protocol does not have a corresponding concrete facade in the list create it.
            if (concreteFacade == null) {
                concreteFacade = (FacadeManagerBase) ((Class) concreteFacadesClasses.get(protocol)).newInstance();
                concreteFacades.put(protocol, concreteFacade);
            }
        }
        catch (Exception e) {
            handleException(e);
        }
        ConnectionStatus status = concreteFacade.initiateConnection();
        if (status != FacadeManagerBase.CONNECTION_STATUS_OK) {
            concreteFacades.remove(protocol);
            inUseProtocols.remove(protocol);
            if (status == FacadeManagerBase.CONNECTION_STATUS_BAD_CREDENTIALS) {
                return status;
            }
            else {
                handleException(new ConnectionStatusException("The connection to " + getDisplayNameForProtocol(protocol) + " failed with message: \n" + status.getStatusMessage(), status.notifyUser()));
                return status;
            }
        }
        if (protocolListeners != null) {
            for (Object protocolListener : protocolListeners) {
                ((InUseProtocolListener) protocolListener).protocolAddedToInUseList(protocol);
            }
        }
        return status;
    }


    static public void removeProtocolFromUseList(String protocol) {
        if (protocolListeners != null && concreteFacades.containsKey(protocol)) {
            InUseProtocolListener[] listeners = (InUseProtocolListener[]) protocolListeners.toArray(new InUseProtocolListener[0]);
            for (InUseProtocolListener listener : listeners) {
                listener.protocolRemovedFromInUseList(protocol);
            }
        }
        getFacadeManager(protocol).prepareForSystemExit();
        if (concreteFacades != null) concreteFacades.remove(protocol);
        inUseProtocols.remove(protocol);
    }

//  static public List getAvailableProtocolStrings() {
//     return new ArrayList(availableProtocols);
//  }

    static public List getInUseProtocolStrings() {
        return new ArrayList(inUseProtocols);
    }

//  static public boolean isAssemblyProtocol(String protocol) {
//     return assemblyProtocols.contains(protocol);
//  }

    static public String getDisplayNameForProtocol(String protocol) {
        return (String) displayNames.get(protocol);
    }

    static public boolean canProtocolAddMoreDataSources(String protocol) {
        FacadeManagerBase base = getFacadeManager(protocol);
        return base.canAddMoreDataSources();
    }


    static public void addInUseProtocolListener(InUseProtocolListener protocolListener) {
        if (protocolListeners == null) protocolListeners = new Vector();
        protocolListeners.add(protocolListener);
        for (Object inUseProtocol : inUseProtocols) {
            protocolListener.protocolAddedToInUseList((String) inUseProtocol);
        }
    }

    static public void removeInUseProtocolListener(InUseProtocolListener protocolListener) {
        if (protocolListeners == null) return;
        protocolListeners.remove(protocolListener);
    }


    static public String getAggregateProtocolString() {
        return AGGREGATE_PROTOCOL;
    }

    static public String getEJBProtocolString() {
        return EJB_PROTOCOL;
    }


    static public DataSourceSelector getDataSourceSelectorForProtocol(String protocol) {
        FacadeManagerBase base = getFacadeManager(protocol);
        String className = base.getDataSourceSelectorClass();
        if (className == null) return null;
        try {
            Class[] supportedInterfaces = Class.forName(className).getInterfaces();
            boolean supported = false;
            for (Class supportedInterface : supportedInterfaces) {
                if (supportedInterface.equals(DataSourceSelector.class)) {
                    supported = true;
                    break;
                }
            }
            if (!supported)
                throw new IllegalStateException("Class " + className + " returned by " + "protocol " + protocol + " as a DataSourceSelector does not support the " + "DataSourceSelector interface.");
            return (DataSourceSelector) Class.forName(className).newInstance();
        }
        catch (Exception ex) {
            throw new IllegalStateException("Class " + className + " returned by " + "protocol " + protocol + " as a DataSourceSelector cannot be " + "instanciated. " + ex.toString());
        }
    }

    static public void registerExceptionHandler(ExceptionHandler handler) {
        if (exceptionHandlers == null) exceptionHandlers = new Vector();
        exceptionHandlers.addElement(handler);
    }

    static public void handleException(Throwable throwable) {
        if (exceptionHandlers == null) return;
        if (throwable instanceof NoDataException) return;
        for (Enumeration e = exceptionHandlers.elements(); e.hasMoreElements(); )
            ((ExceptionHandler) e.nextElement()).handleException(throwable);
    }

    static public void deregisterExceptionHandler(ExceptionHandler handler) {
        if (exceptionHandlers == null) return;
        exceptionHandlers.remove(handler);
    }

    static public boolean isDataSourceConnectivityValid() {
        return PathTranslator.isMounted();
    }
    
    static public String getDataSourceHelpInformation() {
        return PathTranslator.getMountHelpMessage();
    }
    
    static public String getOsSpecificRootPath() {
        return PathTranslator.getOsSpecificRootPath();
    }
}
