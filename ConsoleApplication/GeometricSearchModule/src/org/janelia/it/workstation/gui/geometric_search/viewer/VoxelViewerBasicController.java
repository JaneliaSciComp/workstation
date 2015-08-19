package org.janelia.it.workstation.gui.geometric_search.viewer;

import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Vector3;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.gui.geometric_search.viewer.dataset.Dataset;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.GLDisplayUpdateCallback;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.oitarr.ArrayCubeGLActor;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.oitarr.ArrayCubeShader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL4;
import java.io.File;
import java.util.List;
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

    public void addDataset(Dataset dataset) {
        Runnable addDatasetRunnable=createAddDatasetRunnable(dataset);
        threadPool.submit(addDatasetRunnable);
    }

    Runnable createAddDatasetRunnable(final Dataset dataset) {
        Runnable runnable=new Runnable() {
            @Override
            public void run() {
                dataset.createRenderables();
                model.getDatasetModel().addDataset(dataset);
                selectDataset(dataset);
            }
        };
        return runnable;
    }

    public void selectDataset(Dataset dataset) {
        model.setSelectedDataset(dataset);
        model.setDisposeAndClearAllActorsMsg(); // todo - reimplment with dataset:renderable:actor:glActor design

        viewer.resetView();

    }

//    public List<ArrayCubeGLActor> createCubeGLActorsFromStack(File stackFile, int maxVoxels, List<Vector4> preferredColorList, Matrix4 rotation) {
//        final ArrayCubeShader cubeShader=(ArrayCubeShader)model.getDenseVolumeShaderActionSequence().getShader();
//        logger.info("createCubeActor() calling contructor for ArrayCubeGLActor() for channelIndex="+channelIndex);
//        final ArrayCubeGLActor arrayCubeGLActor =new ArrayCubeGLActor(stackFile, channelIndex, intensityThreshold, maxVoxels);
//        logger.info("past ArrayCubeGLActor constructor for channelIndex="+channelIndex+", starting setup()");
//        arrayCubeGLActor.setup();
//        logger.info("past setup()");
//        arrayCubeGLActor.setColor(color);
//        arrayCubeGLActor.setModel(rotation);
//        final int transparencyQuarterDepth=model.getTransparencyQuarterDepth();
//        arrayCubeGLActor.setUpdateCallback(new GLDisplayUpdateCallback() {
//            @Override
//            public void update(GL4 gl) {
//                Matrix4 view = viewer.getRenderer().getViewMatrix();
//                Matrix4 proj = viewer.getRenderer().getProjectionMatrix();
//                Matrix4 actorModel = arrayCubeGLActor.getModel();
//
//                Matrix4 viewCopy = new Matrix4(view);
//                Matrix4 projCopy = new Matrix4(proj);
//                Matrix4 modelCopy = new Matrix4(actorModel);
//
//                Matrix4 vp = viewCopy.multiply(projCopy);
//                Matrix4 mvp = modelCopy.multiply(vp);
//
//                cubeShader.setMVP(gl, mvp);
//                cubeShader.setProjection(gl, projCopy);
//                cubeShader.setWidth(gl, viewer.getWidth());
//                cubeShader.setHeight(gl, viewer.getHeight());
//                cubeShader.setDepth(gl, transparencyQuarterDepth);
//
//                float voxelUnitSize = arrayCubeGLActor.getVoxelUnitSize();
//                //float voxelUnitSize = 0.01f;
//                cubeShader.setVoxelUnitSize(gl, new Vector3(voxelUnitSize, voxelUnitSize, voxelUnitSize));
//
//                cubeShader.setDrawColor(gl, arrayCubeGLActor.getColor());
//            }
//        });
//        return arrayCubeGLActor;
//    }

}


