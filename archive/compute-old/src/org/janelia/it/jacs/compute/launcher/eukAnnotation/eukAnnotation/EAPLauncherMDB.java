
package org.janelia.it.jacs.compute.launcher.eukAnnotation.eukAnnotation;

import org.janelia.it.jacs.compute.engine.launcher.ejb.SeriesLauncherMDB;
import org.jboss.annotation.ejb.PoolClass;
import org.jboss.ejb3.StrictMaxPool;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;

/**
 * Created by IntelliJ IDEA.
 * User: jinman
 * Date: Jul 26, 2010
 * Time: 12:41:45 PM
 */

@MessageDriven(name="EAPLauncherMDB", activationConfig = {
    @ActivationConfigProperty(propertyName="acknowledgeMode", propertyValue="Auto-acknowledge "),
    @ActivationConfigProperty(propertyName="messagingType", propertyValue="javax.jms.MessageListener"),
    @ActivationConfigProperty(propertyName="destinationType", propertyValue="javax.jms.Queue"),
    @ActivationConfigProperty(propertyName="destination", propertyValue="queue/eapLauncher"),
    @ActivationConfigProperty(propertyName="maxSession", propertyValue="5"),
    @ActivationConfigProperty(propertyName="transactionTimeout", propertyValue="432000"),
    @ActivationConfigProperty(propertyName="DLQMaxResent", propertyValue="0")
})
@PoolClass(value=StrictMaxPool.class, maxSize=5, timeout=10000)
public class EAPLauncherMDB extends SeriesLauncherMDB {

}