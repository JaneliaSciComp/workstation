package org.janelia.it.workstation.gui.geometric_search.search;

import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.workstation.gui.framework.outline.TransferableEntityList;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.geometric_search.viewer.dataset.Dataset;
import org.janelia.it.workstation.model.entity.RootedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.List;

/**
 * Created by murphys on 8/6/2015.
 */
public class MCFODataset extends Dataset {

    private static final Logger logger = LoggerFactory.getLogger(MCFODataset.class);

    File alignedSignalFile;
    File alignedLabelFile;

    public static boolean canImport(Transferable transferable) {
        try {
            List<RootedEntity> rootedEntities = (List<RootedEntity>) transferable.getTransferData(TransferableEntityList.getRootedEntityFlavor());

            if (rootedEntities==null) {
                return false;
            } else {
                int reSize=rootedEntities.size();
            }

            if (rootedEntities.size()>0) {
                RootedEntity re = rootedEntities.get(0);
                String reType=re.getType();
                logger.info("First rooted entity id="+re+" type="+reType);
                RootedEntity alignedStackEntity = re.getChildOfType(EntityConstants.TYPE_ALIGNED_BRAIN_STACK);
                if (alignedStackEntity==null) {
                    logger.info("returned alignedStackEntity is null");
                    return false;
                } else {
                    logger.info("found non-null alignedStackEntity");
                    return true;
                }

            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public static Dataset createDataset(Transferable transferable) {
        return null;
    }

    public boolean createRenderables() {
        return true;
    }

}
