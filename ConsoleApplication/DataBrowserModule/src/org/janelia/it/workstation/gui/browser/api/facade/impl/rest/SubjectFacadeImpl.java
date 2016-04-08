package org.janelia.it.workstation.gui.browser.api.facade.impl.rest;

import java.util.List;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.internal.util.Base64;
import org.janelia.it.jacs.model.domain.Preference;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.shared.utils.DomainQuery;
import org.janelia.it.workstation.gui.browser.api.AccessManager;
import org.janelia.it.workstation.gui.browser.api.facade.interfaces.SubjectFacade;

public class SubjectFacadeImpl extends RESTClientImpl implements SubjectFacade {

    private RESTClientManager manager;
    
    public SubjectFacadeImpl() {
        this.manager = RESTClientManager.getInstance();
    }

    @Override
    public List<Subject> getSubjects() {
        Response response = manager.getUserEndpoint()
                .path("subjects")
                .request("application/json")
                .get();
        if (checkBadResponse(response.getStatus(), "problem making request getSubjects to server")) {
            return null;
        }
        List<Subject> subjects = response.readEntity(new GenericType<List<Subject>>() {
        });
        return subjects;
    }

    @Override
    public Subject getSubjectByKey(String key) {
        Response response = manager.getUserEndpoint()
                .path("subject")
                .queryParam("subjectKey", key)
                .request("application/json")
                .get();
        if (checkBadResponse(response.getStatus(), "problem making request getSubjectByKey to server")) {
            return null;
        }
        Subject subject = response.readEntity(Subject.class);
        return subject;
    }
    
    @Override
    public Subject loginSubject(String username, String password) {
        String credentials = "Basic " + Base64.encodeAsString(username + ":" + password);
        Response response = manager.getLoginEndpoint()
                .request("application/json")
                .header("Authorization", credentials)
                .get();
        if (checkBadResponse(response.getStatus(), "problem making authenticating user against the server")) {
            return null;
        }
        Subject authSubject = response.readEntity(Subject.class);
        return authSubject;
    }

    @Override
    public List<Preference> getPreferences() {
        Response response = manager.getUserEndpoint()
                .path("preferences")
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .request("application/json")
                .get();
        if (checkBadResponse(response.getStatus(), "problem making request getPreferences to server")) {
            return null;
        }
        List<Preference> preferences = response.readEntity(new GenericType<List<Preference>>(){});
        return preferences;
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
            return null;
        }
        Preference newPref = response.readEntity(Preference.class);
        return newPref;
    }
}
