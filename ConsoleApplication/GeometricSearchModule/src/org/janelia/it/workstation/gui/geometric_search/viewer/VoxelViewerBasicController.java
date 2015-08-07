package org.janelia.it.workstation.gui.geometric_search.viewer;

import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Vector3;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.GLDisplayUpdateCallback;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.oitarr.ArrayCubeActor;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.oitarr.ArrayCubeShader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL4;
import java.io.File;
import java.util.concurrent.*;

/**
 * Created by murphys on 7/28/2015.
 */
public class VoxelViewerBasicController implements VoxelViewerController {

    private static final Logger logger = LoggerFactory.getLogger(VoxelViewerBasicController.class);


    LinkedBlockingQueue taskQueue= new LinkedBlockingQueue();
    ThreadPoolExecutor threadPool = new ThreadPoolExecutor(4, 4, 1000L, TimeUnit.MILLISECONDS, taskQueue);

    VoxelViewerModel model;
    VoxelViewerGLPanel viewer;

    public VoxelViewerBasicController() {
    }

    protected void setModel(VoxelViewerModel model) {
        this.model=model;
    }

    protected void setViewer(VoxelViewerGLPanel viewer) {
        this.viewer=viewer;
    }

    public int[] addAlignedStackDataset(final File alignedStackFile) {

        logger.info("addAlignedStackDataset() start");

        final int index0=model.getNextActorIndex();
        final int index1=model.getNextActorIndex();

        threadPool.submit(new Runnable() {
            @Override
            public void run() {
                logger.info("Check0");
                //ArrayCubeActor cubeActor0=createCubeActor(alignedStackFile, 0, 0.20f, 500000, new Vector4(1.0f, 0.0f, 0.0f, 0.05f));
                //cubeActor0.setId(index0);
                logger.info("Check1");
                ArrayCubeActor cubeActor1=createCubeActor(alignedStackFile, 1, 0.25f, 20000000, new Vector4(0.0f, 1.0f, 0.0f, 0.03f));
                cubeActor1.setId(index1);
                logger.info("Check3");

                model.setDisposeAndClearAllActorsMsg();

                logger.info("Check3.1");
                //model.addActorToInitQueue(cubeActor0);
                model.addActorToInitQueue(cubeActor1);
                logger.info("Check3.2");

                viewer.resetView();
            }
        });

        logger.info("addAlignedStackDataset() past submission");

        int[] actorIdArr=new int[2];

        actorIdArr[0]=index0;
        actorIdArr[1]=index1;

        logger.info("returning actorIdArr");

        return actorIdArr;
    }

    public ArrayCubeActor createCubeActor(File stackFile, int channelIndex, float intensityThreshold, int maxVoxels, Vector4 color) {
        final ArrayCubeShader cubeShader=(ArrayCubeShader)model.getDenseVolumeShaderActionSequence().getShader();
        logger.info("createCubeActor() calling contructor for ArrayCubeActor() for channelIndex="+channelIndex);
        final ArrayCubeActor arrayCubeActor=new ArrayCubeActor(stackFile, channelIndex, intensityThreshold, maxVoxels);
        logger.info("past ArrayCubeActor constructor for channelIndex="+channelIndex+", starting setup()");
        arrayCubeActor.setup();
        logger.info("past setup()");
        arrayCubeActor.setColor(color);

        Matrix4 gal4Rotation=new Matrix4();

        gal4Rotation.setTranspose(1.0f, 0.0f, 0.0f, -0.5f,
                0.0f, -1.0f, 0.0f, 0.25f,
                0.0f, 0.0f, -1.0f, 0.625f,
                0.0f, 0.0f, 0.0f, 1.0f);


        arrayCubeActor.setModel(gal4Rotation);
        final int transparencyQuarterDepth=model.getTransparencyQuarterDepth();
        arrayCubeActor.setUpdateCallback(new GLDisplayUpdateCallback() {
            @Override
            public void update(GL4 gl) {
                Matrix4 view = viewer.getRenderer().getViewMatrix();
                Matrix4 proj = viewer.getRenderer().getProjectionMatrix();
                Matrix4 actorModel = arrayCubeActor.getModel();

                Matrix4 viewCopy = new Matrix4(view);
                Matrix4 projCopy = new Matrix4(proj);
                Matrix4 modelCopy = new Matrix4(actorModel);

                Matrix4 vp = viewCopy.multiply(projCopy);
                Matrix4 mvp = modelCopy.multiply(vp);

                cubeShader.setMVP(gl, mvp);
                cubeShader.setProjection(gl, projCopy);
                cubeShader.setWidth(gl, viewer.getWidth());
                cubeShader.setHeight(gl, viewer.getHeight());
                cubeShader.setDepth(gl, transparencyQuarterDepth);

                float voxelUnitSize = arrayCubeActor.getVoxelUnitSize();
                //float voxelUnitSize = 0.01f;
                cubeShader.setVoxelUnitSize(gl, new Vector3(voxelUnitSize, voxelUnitSize, voxelUnitSize));

                cubeShader.setDrawColor(gl, arrayCubeActor.getColor());
            }
        });
        return arrayCubeActor;
    }

}


