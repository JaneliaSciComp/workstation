package org.janelia.workstation.core.api.facade.impl.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.janelia.it.jacs.shared.utils.DomainQuery;
import org.janelia.workstation.core.api.facade.interfaces.SubjectFacade;
import org.janelia.workstation.core.api.http.RESTClientBase;
import org.janelia.workstation.core.api.http.RestJsonClientManager;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.model.domain.Preference;
import org.janelia.model.security.Group;
import org.janelia.model.security.Subject;
import org.janelia.model.security.User;
import org.janelia.model.security.UserGroupRole;
import org.janelia.model.security.dto.AuthenticationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubjectFacadeImpl extends RESTClientBase implements SubjectFacade {

    private static final Logger LOG = LoggerFactory.getLogger(SubjectFacadeImpl.class);

    private static final String REMOTE_API_URL = ConsoleProperties.getInstance().getProperty("domain.facade.rest.url");

    private WebTarget service;
    
    public SubjectFacadeImpl() {
        this(REMOTE_API_URL);
    }

    public SubjectFacadeImpl(String serverUrl) {
        super(LOG);
        LOG.debug("Using server URL: {}",serverUrl);
        this.service = RestJsonClientManager.getInstance().getTarget(serverUrl, true);
    }
    
    @Override
    public List<Subject> getSubjects() throws Exception {
        Response response = service.path("data/user/subjects")
                .request("application/json")
                .get();
        if (checkBadResponse(response.getStatus(), "problem making request getSubjects to server")) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(new GenericType<List<Subject>>() {});
    }

    @Override
    public Subject getSubjectByNameOrKey(String key) throws Exception {
        Response response = service.path("data/user/subject")
                .queryParam("subjectKey", key)
                .request("application/json")
                .get();
        if (checkBadResponse(response.getStatus(), "problem making request getSubjectByNameOrKey to server")) {
            LOG.error("Error getting a subject for {}", key);
            throw new WebApplicationException(response);
        }
        return response.readEntity(Subject.class);
    }
    
    @Override
    public User getOrCreateUser(String username) throws Exception {
        Response response = service.path("data/user/getorcreate")
                .queryParam("subjectKey", "user:"+username)
                .request("application/json")
                .get();
        if (checkBadResponse(response.getStatus(), "problem making getOrCreateUser request against the server")) {
            LOG.error("Error getting or creating a user subject for {}", username);
            throw new WebApplicationException(response);
        }
        return response.readEntity(User.class);
    }

    @Override
    public List<Preference> getPreferences() throws Exception {
        Response response = service.path("data/user/preferences")
                .queryParam("subjectKey", DomainMgr.getPreferenceSubject())
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
        query.setSubjectKey(DomainMgr.getPreferenceSubject());
        query.setPreference(preference);
        Response response = service.path("data/user/preferences")
                .request("application/json")
                .put(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request savePreferences to server: " + preference)) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(Preference.class);
    }



    @Override
    public User updateUser(User user) throws Exception {
        Map<String,Object> paramMap = new HashMap<>();
        paramMap.put("fullname", user.getFullName());
        paramMap.put("name", user.getName());
        paramMap.put("email", user.getEmail());
        Response response = service.path("data/user/property")
                .request("application/json")
                .post(Entity.json(paramMap));
        if (checkBadResponse(response.getStatus(), "problem making request updateUserProperty to server")) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(User.class);
    }

    @Override
    public void updateUserRoles(String userKey, Set<UserGroupRole> userRoles) throws Exception {
        Response response = service.path("data/user/roles")
                .queryParam("userKey", userKey)
                .request("application/json")
                .post(Entity.json(userRoles));
        if (checkBadResponse(response.getStatus(), "problem making request updateUserRoles to server")) {
            throw new WebApplicationException(response);
        }    
    }

    @Override
    public Group createGroup(Group group) throws Exception {
        Response response = service.path("data/group")
                .request("application/json")
                .put(Entity.json(group));
        if (checkBadResponse(response.getStatus(), "problem making request updateUserProperty to server")) {
            throw new WebApplicationException(response);
        }    
        return response.readEntity(Group.class);
    }

    @Override
    public void changeUserPassword (AuthenticationRequest message) throws Exception {
        Response response = service.path("data/user/password")
                .request("application/json")
                .post(Entity.json(message));
        if (checkBadResponse(response.getStatus(), "problem making request to change user password to server")) {
            throw new WebApplicationException(response);
        }
    }
}
