/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

package org.janelia.it.FlyWorkstation.gui.framework.api;

import org.janelia.it.jacs.compute.api.*;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Properties;

/**
 * This class returns interfaces for local EJBs.  It is also used by JUnit tests to return the remote
 * interface
 *
 * @author Tareq Nabeel
 */
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