
package org.janelia.it.jacs.compute.launcher.sift.sift;

import org.janelia.it.jacs.compute.engine.launcher.ejb.SeriesLauncherMDB;
import org.jboss.annotation.ejb.PoolClass;
import org.jboss.ejb3.StrictMaxPool;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;

/**
 * Created by IntelliJ IDEA.
 * User: zguan
 * Date: Jul 21, 2010
 * Time: 10:28:32 AM
 */
@MessageDriven(name = "SiftProteinSubstitutionLauncherMDB", activationConfig = {
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge "),
        @ActivationConfigProperty(propertyName = "messagingType", propertyValue = "javax.jms.MessageListener"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/siftProteinSubstitutionLauncher"),
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "3"),
        @ActivationConfigProperty(propertyName = "transactionTimeout", propertyValue = "432000"),
        @ActivationConfigProperty(propertyName = "DLQMaxResent", propertyValue = "0")
})
@PoolClass(value = StrictMaxPool.class, maxSize = 3, timeout = 10000)
public class SiftProteinSubstitutionLauncherMDB extends SeriesLauncherMDB {

}
