package org.janelia.it.workstation.gui.geometric_search.viewer;

import org.janelia.geometry3d.Matrix4;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.camera.BasicCamera3d;
import org.janelia.it.workstation.gui.camera.BasicObservableCamera3d;
import org.janelia.it.workstation.gui.camera.Camera3d;
import org.janelia.it.workstation.gui.geometric_search.viewer.actor.ActorModel;
import org.janelia.it.workstation.gui.geometric_search.viewer.actor.ActorSharedResource;
import org.janelia.it.workstation.gui.geometric_search.viewer.dataset.Dataset;
import org.janelia.it.workstation.gui.geometric_search.viewer.dataset.DatasetModel;
import org.janelia.it.workstation.gui.geometric_search.viewer.event.EventManager;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.GL4ShaderActionSequence;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.GL4SimpleActor;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.GLDisplayUpdateCallback;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.GLModel;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.oitarr.*;
import org.janelia.it.workstation.gui.geometric_search.viewer.renderable.RenderableModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL4;
import java.util.*;

/**
 * Created by murphys on 4/10/15.
 */
public class VoxelViewerModel {

    private Logger logger = LoggerFactory.getLogger(VoxelViewerModel.class);

    public static final float[] DEFAULT_BACKGROUND_COLOR = {0.0f, 0.0f, 0.3f};
    public static final boolean DEFAULT_SHOWING_AXES = true;

    private Vec3 cameraDepth;
    private Camera3d camera3d;
    private float[] backgroundColor = DEFAULT_BACKGROUND_COLOR;
    private float[] voxelMicrometers;
    private int[] voxelDimensions;
    private boolean showAxes = DEFAULT_SHOWING_AXES;

    VoxelViewerProperties properties;
    VoxelViewerGLPanel viewer;

    DatasetModel datasetModel=new DatasetModel();
    RenderableModel renderableModel=new RenderableModel();
    ActorModel actorModel=new ActorModel();
    GLModel glModel=new GLModel();

    Map<String, ActorSharedResource> actorSharedResourceMap=new HashMap<>();

    public static final double DEFAULT_CAMERA_FOCUS_DISTANCE = 2.0;

    public VoxelViewerModel(VoxelViewerProperties properties) {
        this.properties=properties;
        camera3d = new BasicObservableCamera3d();
        camera3d.setFocus(0.0,0.0,0.5);
        cameraDepth = new Vec3(0.0, 0.0, DEFAULT_CAMERA_FOCUS_DISTANCE);
        setupModelEvents();
    }

    public void setupModelEvents() {
        EventManager.addListener(datasetModel, renderableModel);
        EventManager.addListener(datasetModel, actorModel);
        EventManager.addListener(renderableModel, actorModel);
        EventManager.addListener(actorModel, glModel);
    }

    public ActorModel getActorModel() {
        return actorModel;
    }

    public DatasetModel getDatasetModel() {
        return datasetModel;
    }

    public RenderableModel getRenderableModel() { return renderableModel; }

    public GLModel getGLModel() { return glModel; }

    public void setViewer(VoxelViewerGLPanel viewer) {
        this.viewer=viewer;
    }

    public Camera3d getCamera3d() {
        if ( camera3d == null ) {
            camera3d = new BasicCamera3d();
        }
        return camera3d;
    }

    /** Convenience method to corral this calculation for consistent use. */
    public double getCameraFocusDistance() {
        if ( cameraDepth == null ) {
            return 1.0;
        }
        return cameraDepth.getZ();
    }

    /** Convenience method to corral this calculation for consistent use. */
    public void setCameraPixelsPerSceneUnit( double screenPixelDistance, double cameraFocusDistance ) {
        if ( getCamera3d() == null ) {
            return;
        }
        getCamera3d().setPixelsPerSceneUnit( Math.abs( screenPixelDistance / cameraFocusDistance ) );
    }

    public void setCamera3d(Camera3d camera3d) {
        this.camera3d = camera3d;
    }

    public Vec3 getCameraDepth() {
        return cameraDepth;
    }

    public void setCameraDepth(Vec3 cameraDepth) {
        this.cameraDepth = cameraDepth;
    }

    public float[] getVoxelMicrometers() {
        return voxelMicrometers;
    }

    public void setVoxelMicrometers(float[] voxelMicrometers) {
        this.voxelMicrometers = voxelMicrometers;
    }

    public int[] getVoxelDimensions() {
        return voxelDimensions;
    }

    public void setVoxelDimensions(int[] voxelDimensions) {
        this.voxelDimensions = voxelDimensions;
    }


    /**
     * @return the backgroundColor
     */
    public float[] getBackgroundColorFArr() {
        return backgroundColor;
    }

    /**
     * @param backgroundColor the backgroundColor to set
     */
    public void setBackgroundColor(float[] backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public boolean isShowAxes() {
        return showAxes;
    }

    /**
     * Setting this true allows axes to be visible; false hides them.
     *
     * @param showAxes the showAxes to set
     */
    public void setShowAxes(boolean showAxes) {
        this.showAxes = showAxes;
    }


}
