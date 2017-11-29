package org.janelia.it.workstation.browser.api.facade.impl.rest;

import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.janelia.it.jacs.shared.utils.DomainQuery;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.facade.interfaces.SubjectFacade;
import org.janelia.model.domain.Preference;
import org.janelia.model.security.Subject;
import org.janelia.model.security.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubjectFacadeImpl extends RESTClientImpl implements SubjectFacade {

    private static final Logger log = LoggerFactory.getLogger(SubjectFacadeImpl.class);
    
    public SubjectFacadeImpl() {
        super(log);
    }

    public SubjectFacadeImpl(RESTClientManager manager) {
        super(log, manager);
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
    public User getOrCreateUser(String username) throws Exception {
        Response response = manager.getUserGetOrCreateEndpoint()
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
        Response response = manager.getUserEndpoint()
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
