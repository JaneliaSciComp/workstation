
package org.janelia.it.jacs.compute.launcher.hmmer3.hmmer3;

import org.janelia.it.jacs.compute.engine.launcher.ejb.SeriesLauncherMDB;
import org.jboss.annotation.ejb.PoolClass;
import org.jboss.ejb3.StrictMaxPool;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;


@MessageDriven(name = "HMMER3LauncherMDB", activationConfig = {
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge "),
        @ActivationConfigProperty(propertyName = "messagingType", propertyValue = "javax.jms.MessageListener"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/hmmer3Launcher"),
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "3"),
//    @ActivationConfigProperty(propertyName="MaxMessages", propertyValue="30"),
        @ActivationConfigProperty(propertyName = "transactionTimeout", propertyValue = "432000"),
        // DLQMaxResent is a JBoss-specific management property. 0 = no resent messages
        @ActivationConfigProperty(propertyName = "DLQMaxResent", propertyValue = "0")
})
@PoolClass(value = StrictMaxPool.class, maxSize = 3, timeout = 10000)
public class HMMER3LauncherMDB extends SeriesLauncherMDB
{

    public HMMER3LauncherMDB()
    {
    }
}
