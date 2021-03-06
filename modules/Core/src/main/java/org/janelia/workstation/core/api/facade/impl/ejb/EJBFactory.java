package org.janelia.workstation.core.api.facade.impl.ejb;

import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EJBFactory {
	
    private static final Logger log = LoggerFactory.getLogger(EJBFactory.class);

    private static final String INITIAL_CONTEXT_FACTORY = "org.jnp.interfaces.NamingContextFactory";
    private static final String URL_PKG_PREFIXES = "org.jboss.naming:org.jnp.interfaces";
    private static final String REMOTE_ANNOTATION_JNDI_NAME = "compute/AnnotationEJB/remote";
    private static final String REMOTE_COMPUTE_JNDI_NAME = "compute/ComputeEJB/remote";
    private static final String REMOTE_ENTITY_JNDI_NAME = "compute/EntityEJB/remote";
    private static final String REMOTE_TILED_MICROSCOPE_JNDI_NAME = "compute/TiledMicroscopeEJB/remote";
    private static final String REMOTE_GEOMETRIC_SEARCH_JNDI_NAME = "compute/GeometricSearchEJB/remote";

    /**
     * Create the InitialContext every time to avoid concurrency issues
     *
     * @return InitialContext
     * @throws javax.naming.NamingException problem with the name
     */
    private static InitialContext createInitialContext() throws NamingException {
        String interactiveServer = ConsoleProperties.getInstance().getProperty("interactive.server.url");;

        String interactiveServerUrl = "jnp://"+interactiveServer+":1199";

        log.info("Using interactive server: "+interactiveServerUrl);

        Properties icInteractiveServerProperties = new Properties();
        icInteractiveServerProperties.put(Context.PROVIDER_URL, interactiveServerUrl);
        icInteractiveServerProperties.put(Context.INITIAL_CONTEXT_FACTORY, INITIAL_CONTEXT_FACTORY);
        icInteractiveServerProperties.put(Context.URL_PKG_PREFIXES, URL_PKG_PREFIXES);

        return new InitialContext(icInteractiveServerProperties);
    }
    
    /**
     * @param lookupName the jndi name for the ejb
     * @return EJBObject
     */
    private static Object getRemoteInterface(String lookupName) {
        InitialContext ic=null;
        try {
            ic = createInitialContext();
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

    static ComputeBeanRemote getRemoteComputeBean() {
        return (ComputeBeanRemote) getRemoteInterface(REMOTE_COMPUTE_JNDI_NAME);
    }

}