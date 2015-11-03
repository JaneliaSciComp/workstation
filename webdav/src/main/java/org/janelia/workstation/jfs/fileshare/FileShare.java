package org.janelia.workstation.jfs.fileshare;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.janelia.workstation.jfs.exception.FileNotFoundException;
import org.janelia.workstation.jfs.exception.FileUploadException;
import org.janelia.workstation.jfs.security.Authorizer;
import org.janelia.workstation.jfs.security.Permission;
import org.janelia.workstation.jfs.security.Token;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import java.io.InputStream;

/**
 * Created by schauderd on 6/26/15.
 */
public abstract class FileShare {
    protected String mapping;
    protected Path path;
    protected Authorizer authorizer;
    protected Set<Permission> permissions = new HashSet<>();
    protected boolean isAuthorized = false;

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

    public void init() {

    }

    public StreamingOutput getFile (HttpServletResponse respopnse, String qualifiedFilename) throws FileNotFoundException {
        return null;
    }

    public String propFind (HttpHeaders headers, String filepath) throws FileNotFoundException, IOException {
        return null;
    }

    public void putFile (HttpServletRequest request, HttpServletResponse response, InputStream binaryStream, String filepath) throws FileUploadException {

    }

    public void deleteFile (String qualifiedFilename) throws IOException {

    }

    //  METADATA api
    public StreamingOutput searchFile (HttpServletResponse respopnse, String name) throws FileNotFoundException {
        return null;
    }

    public Object getInfo (HttpServletResponse respopnse, String qualifiedFilename) throws FileNotFoundException {
        return null;
    }

    public void registerFile (HttpServletRequest request, String filepath) throws FileNotFoundException {
    }

    public Map<String,String> registerBulkFiles(HttpServletRequest request, String filestore, List<String> objects) throws FileNotFoundException {
        return null;
    }

    public Map<String,String> generateUsageReports (String store) throws FileNotFoundException {
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
