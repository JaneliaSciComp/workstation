package org.janelia.workstation.webdav;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import org.janelia.workstation.webdav.exception.FileNotFoundException;
import org.janelia.workstation.webdav.exception.FileUploadException;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import java.io.InputStream;

/**
 * Created by schauderd on 6/26/15.
 */
public abstract class FileShare {
    private String mapping;
    private Path path;
    private Authorizer authorizer;
    private Set<Permission> permissions = new HashSet<>();
    private boolean isAuthorized = false;

    public boolean hasAccess (Token credentials) {
        if (!isAuthorized && authorizer != null) {
            try {
                if (authorizer.checkAccess(credentials)) {
                    isAuthorized = true;
                } else {
                    return false;
                }
            } catch (RuntimeException re) {
                re.printStackTrace();
                return false;
            }
        }
        return true;
    }

    public StreamingOutput getFile (HttpServletResponse respopnse, String qualifiedFilename) throws FileNotFoundException {
        return null;
    }

    public String propFind (UriInfo uriInfo, HttpHeaders headers) throws FileNotFoundException, IOException {
        return null;
    }

    public void putFile (InputStream binaryStream, String filepath) throws FileUploadException {

    }

    public void deleteFile (String qualifiedFilename) throws IOException {

    }

    //  METADATA api
    public StreamingOutput getInfo (HttpServletResponse respopnse, String qualifiedFilename) throws FileNotFoundException {
        return null;
    }

    public StreamingOutput searchFile (HttpServletResponse respopnse, String name) throws FileNotFoundException {
        return null;
    }

    public String getMapping() {
        return mapping;
    }

    public void setMapping(String mapping) {
        this.mapping = mapping;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public Authorizer getAuthorizer() {
        return authorizer;
    }

    public void setAuthorizer(Authorizer authorizer) {
        this.authorizer = authorizer;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public void addPermission(Permission permission) {
        this.permissions.add(permission);
    }

    public void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    @Override
    public Object clone() {
        try {
            FileShare userCopy = this.getClass().newInstance();
            userCopy.setPath(this.getPath());
            userCopy.setMapping(this.getMapping());
            userCopy.setAuthorizer(this.getAuthorizer());
            userCopy.setPermissions(this.getPermissions());
            return userCopy;
        } catch (InstantiationException e) {
            e.printStackTrace();
            return null;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }
}
