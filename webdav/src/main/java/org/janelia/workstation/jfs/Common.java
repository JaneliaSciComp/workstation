package org.janelia.workstation.jfs;

import org.glassfish.jersey.internal.util.Base64;
import org.janelia.workstation.jfs.ServicesConfiguration;
import org.janelia.workstation.jfs.security.BasicAuthToken;
import org.janelia.workstation.jfs.fileshare.FileShare;
import org.janelia.workstation.jfs.security.Token;
import org.janelia.workstation.jfs.security.Principal;
import org.janelia.workstation.jfs.exception.FileNotFoundException;
import org.janelia.workstation.jfs.exception.PermissionsFailureException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.HttpHeaders;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by schauderd on 10/5/15.
 */
public class Common {
    static final String API_PATH = "/file";
    public static FileShare checkPermissions(String filepath, HttpHeaders headers, HttpServletRequest request) throws PermissionsFailureException,FileNotFoundException {
        FileShare mapping = mapResource(filepath, request);
        if (mapping.getAuthorizer()!=null) {
            // make sure user has access to this file share; assume basic auth for now
            BasicAuthToken credentials = (BasicAuthToken)getCredentials(headers);
            if (mapping.getAuthorizer() != null && !mapping.hasAccess(credentials)) {
                throw new PermissionsFailureException("Not allowed to access this file share");
            }

            // since the check passed, store the authorized FileShare in the session
            HttpSession session = request.getSession();
            session.setAttribute(mapping.getMapping(), mapping);

            // store the principal in the session for metadata information
            session.setAttribute("principal", new Principal(credentials.getUsername(), credentials));
        }

        return mapping;
    }

    public static Token getCredentials(HttpHeaders headers) throws PermissionsFailureException {
        List<String> authHeaders = headers.getRequestHeader(HttpHeaders.AUTHORIZATION);
        // if no basic auth, return error
        if (authHeaders == null || authHeaders.size() < 1) {
            throw new PermissionsFailureException("Not using basic authentication to access file services");
        }

        // assume Basic request header and scrape username from this;
        // TO DO : handle different authentication methods with a filter populating information for forward
        // propagation
        String encodedBasicAuth = authHeaders.get(0);
        return getBasicAuthUser(encodedBasicAuth);
    }

    public static FileShare mapResource(String filepath, HttpServletRequest request) throws FileNotFoundException {
        Map resourceMap = ServicesConfiguration.getResourcesByMapping();
        Iterator mappings = resourceMap.keySet().iterator();
        FileShare mappedResource = null;
        String bestMatch = null;

        while(true) {
            String session;
            do {
                do {
                    if(!mappings.hasNext()) {
                        if(bestMatch != null) {
                            HttpSession session1 = request.getSession();
                            if(session1.getAttribute(bestMatch) != null) {
                                return (FileShare)session1.getAttribute(bestMatch);
                            }

                            mappedResource = (FileShare)resourceMap.get(bestMatch);
                        }

                        if(mappedResource == null) {
                            throw new FileNotFoundException("no file share mapped for the file requested.");
                        }

                        return (FileShare)mappedResource.clone();
                    }

                    session = (String)mappings.next();
                } while(!filepath.startsWith(session));

            } while(bestMatch != null && session.length() <= bestMatch.length());

            bestMatch = session;
        }
    }

    public static FileShare mapResource(String filepath) {
        Map resourceMap = ServicesConfiguration.getResourcesByMapping();
        Iterator mappings = resourceMap.keySet().iterator();
        FileShare mappedResource = null;
        String bestMatch = "";

        String session = null;
        while (mappings.hasNext()) {
            session = (String) mappings.next();
            if (filepath.startsWith(session)) {
                if (session.length() > bestMatch.length()) {
                    bestMatch = session;
                }
            }
        }

        if (bestMatch.length()>0) {
            mappedResource = (FileShare)resourceMap.get(bestMatch);
            return (FileShare)mappedResource.clone();
        }
        return null;
    }

    public static BasicAuthToken getBasicAuthUser(String basicRequestHeader) {
        String base64Credentials = basicRequestHeader.substring("Basic".length()).trim();
        String credentials = new String(Base64.decodeAsString(base64Credentials));
        BasicAuthToken token = new BasicAuthToken();
        String[] creds = credentials.split(":",2);
        token.setUsername(creds[0]);
        token.setPassword(creds[1]);
        return token;
    }

    public static String stripApiPath (String uriPath) {
       return uriPath.substring(API_PATH.length());
    }
}
