package org.janelia.it.workstation.browser.api.web;

import java.util.Map;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.janelia.it.jacs.model.domain.report.QuotaUsage;
import org.janelia.it.jacs.model.domain.support.SubjectUtils;
import org.janelia.it.workstation.browser.api.facade.impl.rest.RESTClientImpl;
import org.janelia.it.workstation.browser.api.http.RestJsonClientManager;
import org.janelia.it.workstation.browser.util.ConsoleProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RESTful client for getting data from the SAGE Responder web service. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class JFSRestClient extends RESTClientImpl {

    private static final Logger log = LoggerFactory.getLogger(JFSRestClient.class);
    
    private static final String REMOTE_API_URL = ConsoleProperties.getInstance().getProperty("jfs.rest.url");
    private static final String STORE_NAME = "filestore";
    
    private WebTarget service;    

    public JFSRestClient() {
        this(REMOTE_API_URL);
    }
    
    public JFSRestClient(String serverUrl) {
        super(log);
        this.service = RestJsonClientManager.getInstance().getTarget(serverUrl);
    }

    public Map<String,QuotaUsage> getDiskQuotas(String subjectKey) throws Exception {
        String subjectName = SubjectUtils.getSubjectName(subjectKey);
        WebTarget target = service.path("quota").path(STORE_NAME).path("report").path(subjectName);
        Response response = target
                .request("application/json")
                .get();
        try {
            checkBadResponse(target, response);
            return response.readEntity(new GenericType<Map<String,QuotaUsage>>() {});
        }
        finally {
            response.close();
        }
    }

    public QuotaUsage getDiskQuota(String subjectKey) throws Exception {
        String subjectName = SubjectUtils.getSubjectName(subjectKey);
        return getDiskQuotas(subjectKey).get(subjectName);
    }
}
