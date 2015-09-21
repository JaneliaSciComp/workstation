package org.janelia.it.workstation.gui.geometric_search.search;

import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.workstation.gui.framework.outline.TransferableEntityList;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.geometric_search.viewer.VoxelViewer4DImage;
import org.janelia.it.workstation.gui.geometric_search.viewer.VoxelViewerUtil;
import org.janelia.it.workstation.gui.geometric_search.viewer.dataset.Dataset;
import org.janelia.it.workstation.gui.geometric_search.viewer.renderable.DenseVolumeRenderable;
import org.janelia.it.workstation.model.entity.RootedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by murphys on 8/6/2015.
 */
public class MCFODataset extends Dataset {

    private static final Logger logger = LoggerFactory.getLogger(MCFODataset.class);

    List<File> maskFiles=new ArrayList<>();

    public MCFODataset() {
        getNeededActorSharedResources().add(JFRC2010CompartmentSharedResource.getInstance());
    }

    public final Matrix4 getRotation() {

        Matrix4 gal4Rotation=new Matrix4();

        gal4Rotation.setTranspose(1.0f, 0.0f, 0.0f, -0.5f,
                0.0f, -1.0f, 0.0f, 0.25f,
                0.0f, 0.0f, -1.0f, 0.625f,
                0.0f, 0.0f, 0.0f, 1.0f);

        return gal4Rotation;
    }

    public List<File> getMaskFiles() {
        return maskFiles;
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
                logger.info("canImport() first rooted entity id="+re+" type="+reType);

                if (reType.equals(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
                    String name=re.getName();
                    String filePath=re.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                    String pixelResolution=re.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION);
                    logger.info("name="+name);
                    logger.info("filePath="+filePath);
                    logger.info("pixelResolution="+pixelResolution);
                    if (name.equals("Aligned Neuron Separation 20x") &&
                            filePath!=null &&
                            pixelResolution.equals("1024x512x218")) {
                        String testPath=filePath+"/"+"archive"+"/"+"maskChan"+"/"+"neuron_0.mask";
                        File localTestFile=SessionMgr.getCachedFile(testPath, false);
                        if (localTestFile!=null && localTestFile.exists()) {
                            return true;
                        }
                    }
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
            logger.info("createDataset: first rooted entity id="+re+" type="+reType);

            if (reType.equals(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {

                // First, we need to find out how many neurons there are, using entity space because we don't have access to the filesystem
                RootedEntity fragmentCollection = re.getChildOfType(EntityConstants.TYPE_NEURON_FRAGMENT_COLLECTION);
                String separationDirPath = re.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);

                if (fragmentCollection != null) {
                    List<RootedEntity> neuronFragmentList = fragmentCollection.getChildrenOfType(EntityConstants.TYPE_NEURON_FRAGMENT);

                    Entity entity=fragmentCollection.getEntity();

                    if (entity!=null) {

                        Set<Entity> entitySet = entity.getChildren();

                        if (entitySet == null) {
                            logger.info("Based on entity experiment, found " + entitySet.size() + " childrend");
                        } else {
                            if (neuronFragmentList != null) {
                                MCFODataset mcfoDataset = new MCFODataset();
                                int numFragments = entitySet.size();
                                logger.info("Found " + numFragments + " expected neuron fragments based on entity graph");
                                for (int i = 0; i < numFragments; i++) {
                                    String fragmentMaskPath = separationDirPath + "/" + "archive" + "/" + "maskChan" + "/" + "neuron_" + i + ".mask";
                                    logger.info("Looking for file=" + fragmentMaskPath);
                                    File localFile = SessionMgr.getCachedFile(fragmentMaskPath, false);
                                    if (localFile != null) {
                                        mcfoDataset.getMaskFiles().add(localFile);
                                    } else {
                                        logger.info("SessionMgr.getCachedFile() returned null for file=" + fragmentMaskPath);
                                    }
                                }
                                if (mcfoDataset.getMaskFiles().size() > 0) {
                                    logger.info("Returning MCFODataset with " + mcfoDataset.getMaskFiles().size() + " fragments");
                                    return mcfoDataset;
                                } else {
                                    logger.error("Could not find any mask files for MCFODataset, separation dir=" + separationDirPath);
                                }
                            }
                        }

                    } else {
                        logger.info("Entity is null");
                    }

                } else {
                    logger.error("Could not find neuron fragment collection for neuron separator pipeline result");
                }
            } else {
                logger.error("Expected Entity type="+EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT);
                return null;
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public boolean createRenderables() {
        logger.info("createRenderables() start");

        try {




        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }

//        try {
//            VoxelViewer4DImage image = VoxelViewerUtil.createVoxelImageFromStack(alignedStack);
//            float voxelSize = (float)(1.0 / (1.0 * image.getXSize()));
//
//            DenseVolumeRenderable c0r=new DenseVolumeRenderable();
//            if (image.getVoxelByteCount()==1) {
//                c0r.init(image.getXSize(), image.getYSize(), image.getZSize(), voxelSize, image.getData8ForChannel(0));
//            } else {
//                c0r.init(image.getXSize(), image.getYSize(), image.getZSize(), voxelSize, image.getData16ForChannel(0));
//            }
//            setDenseVolumeRenderableName(c0r, 0);
//            c0r.setPreferredColor(new Vector4(1.0f, 0.0f, 0.0f, 0.01f));
//            renderables.add(c0r);
//
//            DenseVolumeRenderable c1r=new DenseVolumeRenderable();
//            c1r.setIntensityThreshold(0.30f);
//            c1r.setMaxVoxels(3000000);
//            if (image.getVoxelByteCount()==1) {
//                c1r.init(image.getXSize(), image.getYSize(), image.getZSize(), voxelSize, image.getData8ForChannel(1));
//            } else {
//                c1r.init(image.getXSize(), image.getYSize(), image.getZSize(), voxelSize, image.getData16ForChannel(1));
//            }
//            c1r.setPreferredColor(new Vector4(0.0f, 1.0f, 0.0f, 0.01f));
//            setDenseVolumeRenderableName(c1r, 1);
//            renderables.add(c1r);
//
//            logger.info("createRenderables() done creating VoxelViewer4DImage");
//        } catch (Exception ex) {
//            ex.printStackTrace();
//            return false;
//        }

        logger.info("createRenderables() end");
        return true;
    }

    protected void setDenseVolumeRenderableName(DenseVolumeRenderable renderable, int channel) {
        Double voxelPerc = (renderable.getSampledVoxelCount() *1.0) / (renderable.getTotalVoxelCount() * 1.0) * 100.0;
        String dString = voxelPerc.toString().substring(0, voxelPerc.toString().indexOf("."));
        renderable.setName("Channel "+channel+" "+dString+"%");
    }

}
