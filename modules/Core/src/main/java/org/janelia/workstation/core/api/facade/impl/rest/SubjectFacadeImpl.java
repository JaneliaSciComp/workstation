package org.janelia.workstation.core.api.facade.impl.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.janelia.it.jacs.shared.utils.DomainQuery;
import org.janelia.model.domain.Preference;
import org.janelia.model.security.Group;
import org.janelia.model.security.Subject;
import org.janelia.model.security.User;
import org.janelia.model.security.UserGroupRole;
import org.janelia.model.security.dto.AuthenticationRequest;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.facade.interfaces.SubjectFacade;
import org.janelia.workstation.core.api.http.RESTClientBase;
import org.janelia.workstation.core.api.http.RestJsonClientManager;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubjectFacadeImpl extends RESTClientBase implements SubjectFacade {

    private static final Logger log = LoggerFactory.getLogger(SubjectFacadeImpl.class);

    private WebTarget service;
    
    public SubjectFacadeImpl(String serverUrl) {
        super(log);
        log.debug("Using server URL: {}",serverUrl);
        this.service = RestJsonClientManager.getInstance().getTarget(serverUrl, true);
    }
    
    @Override
    public List<Subject> getSubjects() throws Exception {
        WebTarget target = service.path("data/user/subjects");
        Response response = target
                .request("application/json")
                .get();
        checkBadResponse(target, response);
        return response.readEntity(new GenericType<List<Subject>>() {});
    }

    @Override
    public Subject getSubjectByNameOrKey(String key) throws Exception {
        WebTarget target = service.path("data/user/subject");
        Response response = target
                .queryParam("subjectKey", key)
                .request("application/json")
                .get();
        checkBadResponse(target, response);
        return response.readEntity(Subject.class);
    }
    
    @Override
    public User getOrCreateUser(String username) throws Exception {
        WebTarget target = service.path("data/user/getorcreate");
        Response response = target
                .queryParam("subjectKey", "user:"+username)
                .request("application/json")
                .get();
        checkBadResponse(target, response);
        return response.readEntity(User.class);
    }

    @Override
    public List<Preference> getPreferences() throws Exception {
        WebTarget target = service.path("data/user/preferences");
        Response response = target
                .queryParam("subjectKey", DomainMgr.getPreferenceSubject())
                .request("application/json")
                .get();
        checkBadResponse(target, response);
        return response.readEntity(new GenericType<List<Preference>>(){});
    }

    @Override
    public Preference savePreference(Preference preference) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(DomainMgr.getPreferenceSubject());
        query.setPreference(preference);
        WebTarget target = service.path("data/user/preferences");
        Response response = target
                .request("application/json")
                .put(Entity.json(query));
        checkBadResponse(target, response);
        return response.readEntity(Preference.class);
    }



    @Override
    public User updateUser(User user) throws Exception {
        Map<String,Object> paramMap = new HashMap<>();
        paramMap.put("fullname", user.getFullName());
        paramMap.put("name", user.getName());
        paramMap.put("email", user.getEmail());
        WebTarget target = service.path("data/user/property");
        Response response = target
                .request("application/json")
                .post(Entity.json(paramMap));
        checkBadResponse(target, response);
        return response.readEntity(User.class);
    }

    @Override
    public void updateUserRoles(String userKey, Set<UserGroupRole> userRoles) throws Exception {
        WebTarget target = service.path("data/user/roles");
        Response response = target
                .queryParam("userKey", userKey)
                .request("application/json")
                .post(Entity.json(userRoles));
        checkBadResponse(target, response);
    }

    @Override
    public Group createGroup(Group group) throws Exception {
        WebTarget target = service.path("data/group");
        Response response = target
                .request("application/json")
                .put(Entity.json(group));
        checkBadResponse(target, response);
        return response.readEntity(Group.class);
    }

    @Override
    public User changeUserPassword(String username, String plaintextPassword) throws Exception {
        AuthenticationRequest message = new AuthenticationRequest();
        message.setUsername(username);
        message.setPassword(plaintextPassword);
        WebTarget target = service.path("data/user/password");
        Response response = target
                .request("application/json")
                .post(Entity.json(message));
        checkBadResponse(target, response);
        return response.readEntity(User.class);
    }
}
