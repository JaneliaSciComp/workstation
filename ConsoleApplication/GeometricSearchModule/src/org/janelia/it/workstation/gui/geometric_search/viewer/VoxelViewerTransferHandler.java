package org.janelia.it.workstation.gui.geometric_search.viewer;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.api.entity_model.management.ModelMgrUtils;
import org.janelia.it.workstation.gui.framework.outline.EntityTransferHandler;
import org.janelia.it.workstation.gui.framework.outline.TransferableEntityList;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.model.domain.AlignmentContext;
import org.janelia.it.workstation.model.domain.EntityWrapperFactory;
import org.janelia.it.workstation.model.domain.Sample;
import org.janelia.it.workstation.model.entity.RootedEntity;
import org.janelia.it.workstation.model.viewer.AlignmentBoardContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * Created by murphys on 7/30/2015.
 */

// Based on Les' AlignmentBoardEntityTransferHandler - which has nice code for handling alignment space disambiguation

public class VoxelViewerTransferHandler extends EntityTransferHandler {

    private final Logger logger = LoggerFactory.getLogger(VoxelViewerTransferHandler.class);

    private JComponent dropTarget;

    public VoxelViewerTransferHandler( JComponent viewer ) {
        this.dropTarget = viewer;
    }

    @Override
    public JComponent getDropTargetComponent() {
        return dropTarget;
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport support) {

        logger.info("canImport() 2 called");

        // Get the target entity.
        Transferable transferable = support.getTransferable();

        logger.info("Check3");

        try {
            List<RootedEntity> rootedEntities = (List<RootedEntity>) transferable.getTransferData(TransferableEntityList.getRootedEntityFlavor());

            if (rootedEntities==null) {
                logger.info("rootedEntities is null");
                return false;
            } else {
                int reSize=rootedEntities.size();
                logger.info("rootedEntities size="+reSize);
            }

            if (rootedEntities.size()>0) {
                RootedEntity re = rootedEntities.get(0);
                String reType=re.getType();
                logger.info("First rooted entity id="+re+" type="+reType);
                RootedEntity alignedStackEntity = re.getChildOfType(EntityConstants.TYPE_ALIGNED_BRAIN_STACK);
                if (alignedStackEntity!=null) {
                    return true;
                } else {
                    return false;
                }

            } else {
                logger.info("Rooted entity list is empty from drag and drop");
            }


        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean importData(final TransferHandler.TransferSupport support) {
        
        logger.info("importData() called");
        
        Transferable transferable = support.getTransferable();

        try {
            List<RootedEntity> rootedEntities = (List<RootedEntity>) transferable.getTransferData(TransferableEntityList.getRootedEntityFlavor());

            RootedEntity re = rootedEntities.get(0);
            String reType=re.getType();
            logger.info("First rooted entity id="+re+" type="+reType);
            RootedEntity alignedStackEntity = re.getChildOfType(EntityConstants.TYPE_ALIGNED_BRAIN_STACK);

            if (alignedStackEntity==null) {
                logger.info("alignedStackEntity is null");
            } else {
                EntityData filePathED = alignedStackEntity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                if (filePathED == null) {
                    logger.info("filePathED is null");
                } else {
                    logger.info("aligned stack path=" + filePathED.getValue());
                    File alignedStackFile=new File(filePathED.getValue());
                    if (alignedStackFile.exists()) {
                        logger.info("stack exists");
                    } else {
                        logger.info("Could not find stack file="+alignedStackFile.getAbsolutePath()+", so trying utility");
                        File utilFile=VoxelViewerUtil.findFile(filePathED.getValue());
                        if (utilFile==null) {
                            logger.info("utility did not succeed");
                        }
                    }
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
        logger.info("at end of importData()");

        return true;
    }

}
