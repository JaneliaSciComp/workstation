package org.janelia.it.FlyWorkstation.api.facade.concrete_facade.ejb;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.janelia.it.FlyWorkstation.gui.util.ConsoleProperties;
import org.janelia.it.jacs.compute.api.*;
import org.janelia.it.jacs.shared.utils.StringUtils;

public class EJBFactory {
    private static final String PROVIDER_URL = ConsoleProperties.getInstance().getProperty("provider.url");
    private static final String PIPELINE_PROVIDER_URL = ConsoleProperties.getInstance().getProperty("pipeline.provider.url");
    private static final String INITIAL_CONTEXT_FACTORY = ConsoleProperties.getInstance().getProperty("initial.context.factory");
    private static final String URL_PKG_PREFIXES = ConsoleProperties.getInstance().getProperty("url.pkg.prefixes");
    private static final String REMOTE_ANNOTATION_JNDI_NAME = ConsoleProperties.getInstance().getProperty("remote.annotation.jndi.name");
    private static final String REMOTE_SOLR_JNDI_NAME = ConsoleProperties.getInstance().getProperty("remote.solr.jndi.name");
    private static final String REMOTE_COMPUTE_JNDI_NAME = ConsoleProperties.getInstance().getProperty("remote.compute.jndi.name");
    private static final String REMOTE_ENTITY_JNDI_NAME = ConsoleProperties.getInstance().getProperty("remote.entity.jndi.name");
    private static final String REMOTE_SEARCH_JNDI_NAME = ConsoleProperties.getInstance().getProperty("remote.search.jndi.name");
    private static final String REMOTE_GENOME_CONTEXT_JNDI_NAME = ConsoleProperties.getInstance().getProperty("remote.genome.context.jndi.name");
    private static final String REMOTE_JOB_CONTROL_JNDI_NAME = ConsoleProperties.getInstance().getProperty("remote.job.control.jndi.name");
    private static final String APPLICATION_SERVER;
    
    private static Properties icProperties = new Properties();
    private static Properties icPropertiesPipelines = new Properties();
    
    
    static {
    	Pattern p = Pattern.compile("\\w+://(.+?)(:(\\d+))?");
    	Matcher m = p.matcher(PROVIDER_URL);
    	if (!m.matches()) {
    		throw new RuntimeException("Cannot parse provider.url");
    	}
    	APPLICATION_SERVER = m.group(1);
    	
        icProperties.put(Context.PROVIDER_URL, PROVIDER_URL);
        icProperties.put(Context.INITIAL_CONTEXT_FACTORY, INITIAL_CONTEXT_FACTORY);
        icProperties.put(Context.URL_PKG_PREFIXES, URL_PKG_PREFIXES);

        if (!StringUtils.isEmpty(PIPELINE_PROVIDER_URL)) {
	        icPropertiesPipelines.put(Context.PROVIDER_URL, PROVIDER_URL);
	        icPropertiesPipelines.put(Context.INITIAL_CONTEXT_FACTORY, INITIAL_CONTEXT_FACTORY);
	        icPropertiesPipelines.put(Context.URL_PKG_PREFIXES, URL_PKG_PREFIXES);
        }
    }

    public static String getAppServerName() {
    	return APPLICATION_SERVER;
    }
    
    /**
     * Create the InitialContext every time to avoid concurrency issues
     *
     * @return InitialContext
     * @throws javax.naming.NamingException problem with the name
     */
    private static InitialContext createInitialContext(boolean remotePipeline) throws NamingException {
    	if (remotePipeline && !StringUtils.isEmpty(PIPELINE_PROVIDER_URL)) {
    		return new InitialContext(icPropertiesPipelines);
    	}
        return new InitialContext(icProperties);
    }
    
    /**
     * @param lookupName the jndi name for the ejb
     * @return EJBObject
     */
    public static Object getRemoteInterface(String lookupName, boolean remotePipeline) {
        try {
            InitialContext ic = createInitialContext(remotePipeline);
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


}