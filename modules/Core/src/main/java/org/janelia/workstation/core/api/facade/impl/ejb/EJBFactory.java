package org.janelia.workstation.core.api.facade.impl.ejb;

import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.janelia.it.jacs.compute.api.AnnotationBeanRemote;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EntityBeanRemote;
import org.janelia.it.jacs.compute.api.GeometricSearchBeanRemote;
import org.janelia.it.jacs.compute.api.TiledMicroscopeBeanRemote;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EJBFactory {
	
    private static final Logger log = LoggerFactory.getLogger(EJBFactory.class);
	
    private static final String INTERACTIVE_SERVER = ConsoleProperties.getInstance().getProperty("interactive.server.url");
    private static final String PIPELINE_SERVER = null;
    private static final String INITIAL_CONTEXT_FACTORY = "org.jnp.interfaces.NamingContextFactory";
    private static final String URL_PKG_PREFIXES = "org.jboss.naming:org.jnp.interfaces";
    private static final String REMOTE_ANNOTATION_JNDI_NAME = "compute/AnnotationEJB/remote";
    private static final String REMOTE_COMPUTE_JNDI_NAME = "compute/ComputeEJB/remote";
    private static final String REMOTE_ENTITY_JNDI_NAME = "compute/EntityEJB/remote";
    private static final String REMOTE_TILED_MICROSCOPE_JNDI_NAME = "compute/TiledMicroscopeEJB/remote";
    private static final String REMOTE_GEOMETRIC_SEARCH_JNDI_NAME = "compute/GeometricSearchEJB/remote";

    private static final Properties icInteractiveServerProperties = new Properties();
    private static final Properties icPipelineServerProperties = new Properties();
    
    private static final String interactiveServer;
    private static final String pipelineServer;

    static {
    	interactiveServer = INTERACTIVE_SERVER;
    	pipelineServer = StringUtils.isEmpty(PIPELINE_SERVER) ? interactiveServer : PIPELINE_SERVER;
    	    	
    	String interactiveServerUrl = "jnp://"+interactiveServer+":1199";
    	String pipelineServerUrl = "jnp://"+pipelineServer+":1199";
    	
    	log.info("Using interactive server: "+interactiveServerUrl);
    	log.info("Using pipeline server: "+pipelineServerUrl);
    	
    	icInteractiveServerProperties.clear();
        icInteractiveServerProperties.put(Context.PROVIDER_URL, interactiveServerUrl);
        icInteractiveServerProperties.put(Context.INITIAL_CONTEXT_FACTORY, INITIAL_CONTEXT_FACTORY);
        icInteractiveServerProperties.put(Context.URL_PKG_PREFIXES, URL_PKG_PREFIXES);

    	icPipelineServerProperties.clear();
        icPipelineServerProperties.put(Context.PROVIDER_URL, pipelineServerUrl);
        icPipelineServerProperties.put(Context.INITIAL_CONTEXT_FACTORY, INITIAL_CONTEXT_FACTORY);
        icPipelineServerProperties.put(Context.URL_PKG_PREFIXES, URL_PKG_PREFIXES);
    }

    public static String getAppServerName() {
    	return interactiveServer;
    }
    
    /**
     * Create the InitialContext every time to avoid concurrency issues
     *
     * @return InitialContext
     * @throws javax.naming.NamingException problem with the name
     */
    private static InitialContext createInitialContext(boolean remotePipeline) throws NamingException {
        return new InitialContext(remotePipeline ? icPipelineServerProperties : icInteractiveServerProperties);
    }
    
    /**
     * @param lookupName the jndi name for the ejb
     * @return EJBObject
     */
    public static Object getRemoteInterface(String lookupName, boolean remotePipeline) {
        InitialContext ic=null;
        try {
            ic = createInitialContext(remotePipeline);
            if (!lookupName.startsWith("compute/")) {
                lookupName = "compute/" + lookupName;
            }
            if (!lookupName.endsWith("/remote")) {
                lookupName += "/remote";
            }

            return ic.lookup(lookupName);
        }
        catch (NamingException e) {
            log.error("Error getting remote interface: "+lookupName,e);
            return null;
        }
        finally {
            if (null!=ic) {
                try {
                    ic.close();
                }
                catch (NamingException e) {
                    log.error("Error closing remote interface: "+lookupName,e);
                }
            }
        }
    }

    /**
     * @param lookupName the jndi name for the ejb
     * @return EJBObject
     */
    public static Object getRemoteInterface(String lookupName) {
    	return getRemoteInterface(lookupName, false);
    }
    
    public static AnnotationBeanRemote getRemoteAnnotationBean() {
        return (AnnotationBeanRemote) getRemoteInterface(REMOTE_ANNOTATION_JNDI_NAME);
    }

    public static ComputeBeanRemote getRemoteComputeBean() {
        return (ComputeBeanRemote) getRemoteInterface(REMOTE_COMPUTE_JNDI_NAME);
    }

    public static ComputeBeanRemote getRemoteComputeBean(boolean remotePipeline) {
        return (ComputeBeanRemote) getRemoteInterface(REMOTE_COMPUTE_JNDI_NAME, remotePipeline);
    }
    
    public static EntityBeanRemote getRemoteEntityBean() {
        return (EntityBeanRemote) getRemoteInterface(REMOTE_ENTITY_JNDI_NAME);
    }
    
    public static TiledMicroscopeBeanRemote getRemoteTiledMicroscopeBean() {
        return (TiledMicroscopeBeanRemote) getRemoteInterface(REMOTE_TILED_MICROSCOPE_JNDI_NAME);
    }
    
    public static GeometricSearchBeanRemote getRemoteGeometricSearchBean() {
        return (GeometricSearchBeanRemote) getRemoteInterface(REMOTE_GEOMETRIC_SEARCH_JNDI_NAME);
    }

}