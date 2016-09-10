
package src.org.janelia.it.jacs.compute.service.search;

import org.janelia.it.jacs.compute.engine.launcher.ejb.SeriesLauncherMDB;
import org.jboss.annotation.ejb.PoolClass;
import org.jboss.ejb3.StrictMaxPool;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Oct 26, 2007
 * Time: 3:00:57 PM
 */
@MessageDriven(name = "SearchMDB", activationConfig = {
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge "),
        @ActivationConfigProperty(propertyName = "messagingType", propertyValue = "javax.jms.MessageListener"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/searchLauncher"),
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "5"),
//    @ActivationConfigProperty(propertyName="MaxMessages", propertyValue="5"),
        @ActivationConfigProperty(propertyName = "transactionTimeout", propertyValue = "432000"),
        // DLQMaxResent is a JBoss-specific management property. 0 = no resent messages
        @ActivationConfigProperty(propertyName = "DLQMaxResent", propertyValue = "0")
})
@PoolClass(value = StrictMaxPool.class, maxSize = 5, timeout = 10000)
public class SearchMDB extends SeriesLauncherMDB {

}
