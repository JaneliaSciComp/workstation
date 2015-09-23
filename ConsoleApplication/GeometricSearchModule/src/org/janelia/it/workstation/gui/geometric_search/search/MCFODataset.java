package org.janelia.it.workstation.gui.geometric_search.search;

import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Vector3;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.framework.outline.TransferableEntityList;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.geometric_search.viewer.VoxelViewer4DImage;
import org.janelia.it.workstation.gui.geometric_search.viewer.VoxelViewerUtil;
import org.janelia.it.workstation.gui.geometric_search.viewer.dataset.Dataset;
import org.janelia.it.workstation.gui.geometric_search.viewer.renderable.DenseVolumeRenderable;
import org.janelia.it.workstation.gui.geometric_search.viewer.renderable.SparseVolumeRenderable;
import org.janelia.it.workstation.model.entity.RootedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Created by murphys on 8/6/2015.
 */
public class MCFODataset extends Dataset {

    private static final Logger logger = LoggerFactory.getLogger(MCFODataset.class);

    List<File> maskFiles=new ArrayList<>();
    List<File> chanFiles=new ArrayList<>();

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

    public List<File> getChanFiles() { return chanFiles; }

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

                        Entity sampleEntity = getSampleFromNeuronSeparationPipelineEntity(re);

                        if (sampleEntity==null) {
                            logger.info("sampleEntity is null");
                        } else {
                            logger.info("Sample name="+sampleEntity.getName());
                        }

                        Set<Entity> entitySet = entity.getChildren();

                        if (entitySet == null) {
                            logger.info("Based on entity experiment, found " + entitySet.size() + " childrend");
                        } else {
                            if (neuronFragmentList != null) {
                                MCFODataset mcfoDataset = new MCFODataset();
                                if (sampleEntity!=null) {
                                    mcfoDataset.setName(sampleEntity.getName());
                                } else {
                                    mcfoDataset.setName("unknown sample");
                                }
                                int numFragments = entitySet.size();
                                logger.info("Found " + numFragments + " expected neuron fragments based on entity graph");
                                for (int i = 0; i < numFragments; i++) {
                                    String fragmentBasePath = separationDirPath + "/" + "archive" + "/" + "maskChan" + "/" + "neuron_" + i;
                                    String fragmentMaskPath = fragmentBasePath + ".mask";
                                    String fragmentChanPath = fragmentBasePath + ".chan";
                                    logger.info("Looking for file=" + fragmentMaskPath);
                                    File localFile = SessionMgr.getCachedFile(fragmentMaskPath, false);
                                    if (localFile != null) {
                                        mcfoDataset.getMaskFiles().add(localFile);
                                        File localChanFile = SessionMgr.getCachedFile(fragmentChanPath, false);
                                        mcfoDataset.getChanFiles().add(localChanFile); // OK that may be null
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
            Random random=new Random();
            int i=0;
            for (File maskFile : maskFiles) {
                logger.info("Creating renderable for maskFile="+maskFile.getAbsolutePath());
                SparseVolumeRenderable sparseVolumeRenderable=new SparseVolumeRenderable();
                VoxelViewerUtil.initRenderableFromMaskFile(sparseVolumeRenderable, maskFile, chanFiles.get(i));
                logger.info("Found " + sparseVolumeRenderable.getVoxels().size() + " points in mask file="+maskFile.getAbsolutePath());
                setSparseVolumeRenderableName(sparseVolumeRenderable, maskFile);
                sparseVolumeRenderable.setPreferredColor(new Vector4(random.nextFloat(), random.nextFloat(), random.nextFloat(), 0.3f));
                renderables.add(sparseVolumeRenderable);
                i++;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        logger.info("createRenderables() end");
        return true;
    }

    protected void setSparseVolumeRenderableName(SparseVolumeRenderable renderable, File maskFile) {
        renderable.setName(maskFile.getName());
    }

    private static Entity getSampleFromNeuronSeparationPipelineEntity(RootedEntity re) {
        Entity neuronSepPipelineEntity = re.getEntity();
        try {
            Entity sampleEntity=ModelMgr.getModelMgr().getAncestorWithType(neuronSepPipelineEntity, EntityConstants.TYPE_SAMPLE);
            return sampleEntity;
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(ex.toString());
            return null;
        }
    }


}
