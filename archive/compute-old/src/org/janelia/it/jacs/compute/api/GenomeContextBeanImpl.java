
package org.janelia.it.jacs.compute.api;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.GenomeContextDAO;
import org.janelia.it.jacs.model.metadata.Sample;
import org.jboss.annotation.ejb.PoolClass;
import org.jboss.annotation.ejb.TransactionTimeout;
import org.jboss.ejb3.StrictMaxPool;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Nov 12, 2007
 * Time: 11:22:58 AM
 * From jacs.properties
 * GenomeContextEJB.Name=GenomeContextEJB
 */
@Stateless(name = "GenomeContextEJB")
@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
@TransactionTimeout(432000)
@PoolClass(value = StrictMaxPool.class, maxSize = 100, timeout = 10000)
public class GenomeContextBeanImpl implements org.janelia.it.jacs.compute.api.GenomeContextBeanLocal, GenomeContextBeanRemote {
    private Logger _logger = Logger.getLogger(this.getClass());
    public static final String APP_VERSION = "jacs.version";
    public static final String SEARCH_EJB_PROP = "GenomeContextEJB.Name";
    public static final String MDB_PROVIDER_URL_PROP = "AsyncMessageInterface.ProviderURL";

    private GenomeContextDAO _genomeContextDAO = new GenomeContextDAO(_logger);

    public GenomeContextBeanImpl() {
    }

    public List<Sample> getSamplesByProject(String projectId) throws Exception {
        return _genomeContextDAO.getSamplesByProject(projectId);
    }

}
