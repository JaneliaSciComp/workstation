package org.janelia.it.workstation.gui.geometric_search.search;

import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.workstation.gui.framework.outline.EntityTransferHandler;
import org.janelia.it.workstation.gui.framework.outline.TransferableEntityList;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.geometric_search.viewer.VoxelViewerController;
import org.janelia.it.workstation.model.entity.RootedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.List;

/**
 * Created by murphys on 7/31/2015.
 */

public class GeometricSearchTransferHandler extends EntityTransferHandler {

    private final Logger logger = LoggerFactory.getLogger(GeometricSearchTransferHandler.class);

    private JComponent dropTarget;
    private VoxelViewerController controller;

    public GeometricSearchTransferHandler(JComponent dropTarget, VoxelViewerController controller) {
        this.dropTarget = dropTarget;
        this.controller = controller;
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
                if (alignedStackEntity==null) {
                    logger.info("returned alignedStackEntity is null");
                    return false;
                } else {
                    logger.info("found non-null alignedStackEntity");
                    return true;
                }

            } else {
                logger.info("Rooted entity list is empty from drag and drop");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        logger.info("canImport() returning false by default");
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
                    File localFile = SessionMgr.getCachedFile(filePathED.getValue(), false);
                    if (!localFile.exists()) {
                        throw new Exception("SessionMgr.getCachedFile() failed to retrieve file="+filePathED.getValue());
                    } else {
                        logger.info("file="+filePathED.getValue()+" successfully found");
                        int datasetId=controller.addAlignedStackDataset(localFile);
                        logger.info("Added stack file="+filePathED.getValue()+" assigned datasetId="+datasetId);
                    }
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return true;
    }

}
