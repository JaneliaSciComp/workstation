package org.janelia.it.workstation.browser.api.facade.impl.rest;

import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.janelia.it.jacs.shared.utils.DomainQuery;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.facade.interfaces.SubjectFacade;
import org.janelia.it.workstation.browser.api.http.RESTClientBase;
import org.janelia.it.workstation.browser.api.http.RestJsonClientManager;
import org.janelia.model.domain.Preference;
import org.janelia.model.security.Subject;
import org.janelia.model.security.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubjectFacadeImpl extends RESTClientBase implements SubjectFacade {

    private static final Logger log = LoggerFactory.getLogger(SubjectFacadeImpl.class);

    private static final String REMOTE_API_URL = ConsoleApp.getConsoleApp().getRemoteRestUrl();

    private WebTarget service;
    
    public SubjectFacadeImpl() {
        this(REMOTE_API_URL);
    }

    public SubjectFacadeImpl(String serverUrl) {
        super(log);
        log.debug("Using server URL: {}",serverUrl);
        this.service = RestJsonClientManager.getInstance().getTarget(serverUrl, true);
    }
    
    @Override
    public List<Subject> getSubjects() throws Exception {
        Response response = service.path("data/user")
                .path("subjects")
                .request("application/json")
                .get();
        if (checkBadResponse(response.getStatus(), "problem making request getSubjects to server")) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(new GenericType<List<Subject>>() {});
    }

    @Override
    public Subject getSubjectByNameOrKey(String key) throws Exception {
        Response response = service.path("data/user")
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
    public User getOrCreateUser(String username) throws Exception {
        Response response = service.path("data/user/getorcreate")
                .queryParam("subjectKey", "user:"+username)
                .request("application/json")
                .get();
        if (checkBadResponse(response.getStatus(), "problem making user request against the server")) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(User.class);
    }

    @Override
    public List<Preference> getPreferences() throws Exception {
        Response response = service.path("data/user")
                .path("preferences")
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
        Response response = service.path("data/user")
                .path("preferences")
                .request("application/json")
                .put(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making saving request savePreferences to server: " + preference)) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(Preference.class);
    }
}
