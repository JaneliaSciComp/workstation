package org.janelia.workstation.webdav;

import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by schauderd on 6/26/15.
 */
public abstract class FileShare {
    private String mapping;
    private Path path;
    private Authorizer authorizer;
    private Set<Permission> permissions = new HashSet<>();

    public boolean hasAccess (Token credentials, Permission reqPermission) {
        if (authorizer != null) {
            try {
                return authorizer.checkAccess(credentials) && permissions.contains(reqPermission);
            } catch (RuntimeException re) {
                re.printStackTrace();
                return false;
            }
        }
        return true;
    }

    public void getFile (OutputStream response, String qualifiedFilename) {
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

}
