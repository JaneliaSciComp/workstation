package org.janelia.it.workstation.browser.api.facade.impl.rest;

import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.internal.util.Base64;
import org.janelia.it.jacs.model.domain.Preference;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.subjects.Group;
import org.janelia.it.jacs.model.domain.subjects.GroupRole;
import org.janelia.it.jacs.model.domain.subjects.User;
import org.janelia.it.jacs.shared.utils.DomainQuery;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.api.facade.interfaces.SubjectFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubjectFacadeImpl extends RESTClientImpl implements SubjectFacade {

    private static final Logger log = LoggerFactory.getLogger(SubjectFacadeImpl.class);
    private RESTClientManager manager;
    
    public SubjectFacadeImpl() {
        super(log);
        this.manager = RESTClientManager.getInstance();
    }

    @Override
    public List<Subject> getSubjects() throws Exception {
        Response response = manager.getUserEndpoint()
                .path("subjects")
                .request("application/json")
                .get();
        if (checkBadResponse(response.getStatus(), "problem making request getSubjects to server")) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(new GenericType<List<Subject>>() {});
    }

    @Override
    public Subject getSubjectByKey(String key) throws Exception {
        Response response = manager.getUserEndpoint()
                .path("subject")
                .queryParam("subjectKey", key)
                .request("application/json")
                .get();
        if (checkBadResponse(response.getStatus(), "problem making request getSubjectByKey to server")) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(Subject.class);
    }

    @Override
    public User addUserGroupRole(User user, Group group, GroupRole groupRole) throws Exception {
        Response response = manager.getUserEndpoint()
                .path("addRole")
                .queryParam("subjectKey", AccessManager.getAccessManager().getAuthenticatedSubject().getKey())
                .queryParam("userNameOrKey", user.getKey())
                .queryParam("groupNameOrKey", group.getKey())
                .queryParam("groupRole", groupRole.name())
                .request("application/json")
                .get();
        if (checkBadResponse(response.getStatus(), "problem making request to addRole on server")) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(User.class);
    }

    @Override
    public User removeUserGroupRoles(User user, Group group) throws Exception {
        Response response = manager.getUserEndpoint()
                .path("removeRoles")
                .queryParam("subjectKey", AccessManager.getAccessManager().getAuthenticatedSubject().getKey())
                .queryParam("userNameOrKey", user.getKey())
                .queryParam("groupNameOrKey", group.getKey())
                .request("application/json")
                .get();
        if (checkBadResponse(response.getStatus(), "problem making request to removeRoles on server")) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(User.class);
    }

    @Override
    public <T extends Subject> T save(T subject) throws Exception {
        Response response = manager.getUserEndpoint().path("subject")
                .queryParam("subjectKey", AccessManager.getAccessManager().getAuthenticatedSubject().getKey())
                .request("application/json")
                .put(Entity.json(subject));
        if (checkBadResponse(response.getStatus(), "problem making request to save subject on server: " + subject.getId())) {
            throw new WebApplicationException(response);
        }
        return (T)response.readEntity(Subject.class);
    }
    
    @Override
    public Subject loginSubject(String username, String password) throws Exception {
        String credentials = "Basic " + Base64.encodeAsString(username + ":" + password);
        Response response = manager.getLoginEndpoint()
                .request("application/json")
                .header("Authorization", credentials)
                .get();
        if (checkBadResponse(response.getStatus(), "problem making authenticating user against the server")) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(Subject.class);
    }

    @Override
    public List<Preference> getPreferences() throws Exception {
        Response response = manager.getUserEndpoint()
                .path("preferences")
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .request("application/json")
                .get();
        if (checkBadResponse(response.getStatus(), "problem making request getPreferences to server")) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(new GenericType<List<Preference>>(){});
    }

    @Override
    public Preference savePreference(Preference preference) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setPreference(preference);
        Response response = manager.getUserEndpoint()
                .path("preferences")
                .request("application/json")
                .put(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making saving request savePreferences to server: " + preference)) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(Preference.class);
    }
}
