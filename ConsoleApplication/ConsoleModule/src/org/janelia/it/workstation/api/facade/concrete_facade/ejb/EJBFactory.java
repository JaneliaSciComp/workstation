package org.janelia.it.workstation.api.facade.concrete_facade.ejb;

import org.janelia.it.workstation.gui.framework.session_mgr.SessionModel;
import org.janelia.it.workstation.shared.util.ConsoleProperties;
import org.janelia.it.jacs.compute.api.*;
import org.janelia.it.jacs.compute.api.GeometricSearchBeanRemote;
import org.janelia.it.jacs.compute.api.EntityBeanRemote;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Properties;

public class EJBFactory {
	
	private static final Logger log = LoggerFactory.getLogger(EJBFactory.class);
	
//    private static final String DEFAULT_INTERACTIVE_SERVER = ConsoleProperties.getInstance().getProperty("default.interactive.server.url");
//    private static final String DEFAULT_PIPELINE_SERVER = ConsoleProperties.getInstance().getProperty("default.pipeline.server.url");
    private static final String INTERACTIVE_SERVER = ConsoleProperties.getInstance().getProperty("interactive.server.url");
    private static final String PIPELINE_SERVER = ConsoleProperties.getInstance().getProperty("pipeline.server.url");
    private static final String INITIAL_CONTEXT_FACTORY = ConsoleProperties.getInstance().getProperty("initial.context.factory");
    private static final String URL_PKG_PREFIXES = ConsoleProperties.getInstance().getProperty("url.pkg.prefixes");
    private static final String REMOTE_ANNOTATION_JNDI_NAME = ConsoleProperties.getInstance().getProperty("remote.annotation.jndi.name");
    private static final String REMOTE_SOLR_JNDI_NAME = ConsoleProperties.getInstance().getProperty("remote.solr.jndi.name");
    private static final String REMOTE_COMPUTE_JNDI_NAME = ConsoleProperties.getInstance().getProperty("remote.compute.jndi.name");
    private static final String REMOTE_ENTITY_JNDI_NAME = ConsoleProperties.getInstance().getProperty("remote.entity.jndi.name");
    private static final String REMOTE_SEARCH_JNDI_NAME = ConsoleProperties.getInstance().getProperty("remote.search.jndi.name");
    private static final String REMOTE_GENOME_CONTEXT_JNDI_NAME = ConsoleProperties.getInstance().getProperty("remote.genome.context.jndi.name");
    private static final String REMOTE_JOB_CONTROL_JNDI_NAME = ConsoleProperties.getInstance().getProperty("remote.job.control.jndi.name");
    private static final String REMOTE_TILED_MICROSCOPE_JNDI_NAME = ConsoleProperties.getInstance().getProperty("remote.tiled.microscope.jndi.name");
    private static final String REMOTE_GEOMETRIC_SEARCH_JNDI_NAME = ConsoleProperties.getInstance().getProperty("remote.geometric_search.jndi.name");

    private static Properties icInteractiveServerProperties = new Properties();
    private static Properties icPipelineServerProperties = new Properties();
    
    private static String interactiveServer;
    private static String pipelineServer;

    
    public static void initFromModelProperties(SessionModel sessionModel) {
    	
//    	interactiveServer = (String)sessionModel.getModelProperty(SessionMgr.JACS_INTERACTIVE_SERVER_PROPERTY);
//    	pipelineServer = (String)sessionModel.getModelProperty(SessionMgr.JACS_PIPELINE_SERVER_PROPERTY);
//    	
//    	if (interactiveServer==null) {
//    		interactiveServer = DEFAULT_INTERACTIVE_SERVER;
//    		sessionModel.setModelProperty(SessionMgr.JACS_INTERACTIVE_SERVER_PROPERTY, interactiveServer);
//    	}
//    	
//    	if (pipelineServer==null) {
//    		pipelineServer = DEFAULT_PIPELINE_SERVER;
//    		sessionModel.setModelProperty(SessionMgr.JACS_PIPELINE_SERVER_PROPERTY, pipelineServer);
//    	}
    	
    	interactiveServer = INTERACTIVE_SERVER;
    	pipelineServer = PIPELINE_SERVER;
    	
    	if (StringUtils.isEmpty(pipelineServer)) {
    		pipelineServer = interactiveServer;
    	}
    	
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
            e.printStackTrace();
            return null;
        }
        finally {
            if (null!=ic) {
                try {
                    ic.close();
                }
                catch (NamingException e) {
                    e.printStackTrace();
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
    
    /**
     * @return ComputeBeanRemote
     */
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
    
    public static SearchBeanRemote getRemoteSearchBean() {
        return (SearchBeanRemote) getRemoteInterface(REMOTE_SEARCH_JNDI_NAME);
    }

    public static SolrBeanRemote getRemoteSolrBean() {
        return (SolrBeanRemote) getRemoteInterface(REMOTE_SOLR_JNDI_NAME);
    }

    public static GenomeContextBeanRemote getRemoteGenomeContextBean() {
        return (GenomeContextBeanRemote) getRemoteInterface(REMOTE_GENOME_CONTEXT_JNDI_NAME);
    }

    public static JobControlBeanRemote getRemoteJobControlBean() {
        return (JobControlBeanRemote) getRemoteInterface(REMOTE_JOB_CONTROL_JNDI_NAME);
    }

    public static TiledMicroscopeBeanRemote getRemoteTiledMicroscopeBean() {
        return (TiledMicroscopeBeanRemote) getRemoteInterface(REMOTE_TILED_MICROSCOPE_JNDI_NAME);
    }

    public static GeometricSearchBeanRemote getRemoteGeometricSearchBean() {
        return (GeometricSearchBeanRemote) getRemoteInterface(REMOTE_GEOMETRIC_SEARCH_JNDI_NAME);
    }


}