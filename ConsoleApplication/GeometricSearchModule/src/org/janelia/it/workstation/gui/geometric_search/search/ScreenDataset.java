package org.janelia.it.workstation.gui.geometric_search.search;

import org.janelia.geometry3d.Matrix4;
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
public class ScreenDataset extends Dataset {

    private static final Logger logger = LoggerFactory.getLogger(ScreenDataset.class);

    File alignedStack;

    public void setAlignedStack(File alignedStack) {
        this.alignedStack=alignedStack;
    }

    public final Matrix4 getRotation() {

        Matrix4 gal4Rotation=new Matrix4();

        gal4Rotation.setTranspose(1.0f, 0.0f, 0.0f, -0.5f,
                0.0f, -1.0f, 0.0f, 0.25f,
                0.0f, 0.0f, -1.0f, 0.625f,
                0.0f, 0.0f, 0.0f, 1.0f);

        return gal4Rotation;
    }

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
                        File alignedStack = new File(filePathED.getValue());
                        ScreenDataset screenDataset = new ScreenDataset();
                        EntityData nameEd=alignedStackEntity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_NAME);
                        if (nameEd!=null) {
                            screenDataset.setName(nameEd.getValue());
                        }
                        screenDataset.setAlignedStack(alignedStack);
                        logger.info("returning screenDataset");
                        return screenDataset;
                    }
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public boolean createRenderables() {
        logger.info("createRenderables() start");
        
        logger.info("createRenderables() end");
        return true;
    }

}
