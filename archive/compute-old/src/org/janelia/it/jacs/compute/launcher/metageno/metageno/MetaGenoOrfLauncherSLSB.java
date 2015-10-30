
package org.janelia.it.jacs.compute.launcher.metageno.metageno;

import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.def.SeriesDef;
import org.janelia.it.jacs.compute.engine.launcher.ILauncher;
import org.janelia.it.jacs.compute.engine.launcher.LauncherException;
import org.janelia.it.jacs.compute.engine.launcher.ejb.SequenceLauncherSLSB;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.jboss.annotation.ejb.PoolClass;
import org.jboss.annotation.ejb.TransactionTimeout;
import org.jboss.ejb3.StrictMaxPool;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 * @author Sean Murphy based on Tareq Nabeel
 */
@Stateless
@TransactionAttribute(value = TransactionAttributeType.MANDATORY)
@TransactionTimeout(432000)
@PoolClass(value = StrictMaxPool.class, maxSize = 10, timeout = 10000)
public class MetaGenoOrfLauncherSLSB extends SequenceLauncherSLSB {

    /**
     * You don't have to give up and rethrow the exception to the initiator.  You can retry if you really
     * know what you're doing.  Beware of retries.  The transaction might have sunk into a corrupted state.
     * Put different error codes into your Service exception when you throw them within your services and
     * analyze the code in the catch blocks below if you want to go for retries.  ServiceException
     * has an error code data member.
     *
     * @param launcher    The Sequence Launcher instance
     * @param seriesDef   sequence definition
     * @param processData running state of the process
     * @throws LauncherException
     * @throws ServiceException
     */
    protected void launch(ILauncher launcher, SeriesDef seriesDef, IProcessData processData)
            throws ComputeException {
        launcher.launch(seriesDef, processData);
    }
}
