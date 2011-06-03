package org.janelia.it.FlyWorkstation.gui.framework.api;

import org.janelia.it.jacs.compute.api.*;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Properties;

public class EJBFactory {
    private static final String PROVIDER_URL = "jnp://saffordt-ws1.janelia.priv:1199";
    private static final String INITIAL_CONTEXT_FACTORY = "org.jnp.interfaces.NamingContextFactory";
    private static final String URL_PKG_PREFIXES = "org.jboss.naming:org.jnp.interfaces";
    private static final String REMOTE_ANNOTATION_JNDI_NAME = "compute/AnnotationEJB/remote";
    private static final String REMOTE_COMPUTE_JNDI_NAME = "compute/ComputeEJB/remote";
    private static final String REMOTE_SEARCH_JNDI_NAME = "compute/SearchEJB/remote";
    private static final String REMOTE_GENOME_CONTEXT_JNDI_NAME = "compute/GenomeContextEJB/remote";
    private static final String REMOTE_JOB_CONTROL_JNDI_NAME = "compute/JobControlEJB/remote";
    private static Properties icProperties = new Properties();

    static {
        icProperties.put(Context.PROVIDER_URL, PROVIDER_URL);
        icProperties.put(Context.INITIAL_CONTEXT_FACTORY, INITIAL_CONTEXT_FACTORY);
        icProperties.put(Context.URL_PKG_PREFIXES, URL_PKG_PREFIXES);
    }

    /**
     * Create the InitialContext every time to avoid concurrency issues
     *
     * @return InitialContext
     * @throws javax.naming.NamingException problem with the name
     */
    private static InitialContext createInitialContext() throws NamingException {
        return new InitialContext(icProperties);
    }

    /**
     *
     * @param lookupName the jndi name for the ejb
     * @return EJBObject
     */
    public static Object getRemoteInterface(String lookupName) {
        try {
            InitialContext ic = createInitialContext();
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
     *
     * @return ComputeBeanRemote
     */
    public static AnnotationBeanRemote getRemoteAnnotationBean() {
        return (AnnotationBeanRemote) getRemoteInterface(REMOTE_ANNOTATION_JNDI_NAME);
    }

    public static ComputeBeanRemote getRemoteComputeBean() {
        return (ComputeBeanRemote) getRemoteInterface(REMOTE_COMPUTE_JNDI_NAME);
    }

    public static SearchBeanRemote getRemoteSearchBean() {
        return (SearchBeanRemote) getRemoteInterface(REMOTE_SEARCH_JNDI_NAME);
    }

    public static GenomeContextBeanRemote getRemoteGenomeContextBean() {
        return (GenomeContextBeanRemote) getRemoteInterface(REMOTE_GENOME_CONTEXT_JNDI_NAME);
    }

    public static JobControlBeanRemote getRemoteJobControlBean() {
        return (JobControlBeanRemote) getRemoteInterface(REMOTE_JOB_CONTROL_JNDI_NAME);
    }


}