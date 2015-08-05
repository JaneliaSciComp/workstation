package org.janelia.it.workstation.gui.geometric_search.viewer;

import org.janelia.geometry3d.Matrix4;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.camera.BasicCamera3d;
import org.janelia.it.workstation.gui.camera.BasicObservableCamera3d;
import org.janelia.it.workstation.gui.camera.Camera3d;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.GL4ShaderActionSequence;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.GL4SimpleActor;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.GLDisplayUpdateCallback;
import org.janelia.it.workstation.gui.geometric_search.viewer.gl.oitarr.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL4;
import javax.media.opengl.GLDrawable;
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

    public static final String DISPOSE_AND_CLEAR_ALL_ACTORS_MSG = "DISPOSE_AND_CLEAR_ALL_ACTORS_MSG";

    protected GL4ShaderActionSequence denseVolumeShaderActionSequence = new GL4ShaderActionSequence("Dense Volumes");
    protected GL4ShaderActionSequence meshShaderActionSequence = new GL4ShaderActionSequence("Meshes");

    protected Deque<GL4SimpleActor> initQueue = new ArrayDeque<>();
    protected Deque<Integer> disposeQueue = new ArrayDeque<>();
    protected Deque<String> messageQueue = new ArrayDeque<>();

    VoxelViewerProperties properties;
    VoxelViewerGLPanel viewer;

    public static final double DEFAULT_CAMERA_FOCUS_DISTANCE = 2.0;

    public int maxActorIndex=0;

    public VoxelViewerModel(VoxelViewerProperties properties) {
        this.properties=properties;
        camera3d = new BasicObservableCamera3d();
        camera3d.setFocus(0.0,0.0,0.5);
        cameraDepth = new Vec3(0.0, 0.0, DEFAULT_CAMERA_FOCUS_DISTANCE);
    }

    public int getNextActorIndex() {
        synchronized (this) {
            int index=maxActorIndex;
            maxActorIndex++;
            return index;
        }
    }

    public void setViewer(VoxelViewerGLPanel viewer) {
        this.viewer=viewer;
    }

    public void postViewerIntegrationSetup() {
        setupDenseVolumeShader();
        setupMeshShader();
    }

    public int getTransparencyQuarterDepth() {
        int transparencyQuarterDepth=0;
        try {
            transparencyQuarterDepth=properties.getInteger(VoxelViewerProperties.GL_TRANSPARENCY_QUARTERDEPTH_INT);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return transparencyQuarterDepth;
    }

    private void setupDenseVolumeShader() {
        final ArrayCubeShader arrayCubeShader = new ArrayCubeShader(properties);
        final Integer transparencyQuarterDepth=getTransparencyQuarterDepth();
        arrayCubeShader.setUpdateCallback(new GLDisplayUpdateCallback() {
            @Override
            public void update(GL4 gl) {
                arrayCubeShader.setWidth(gl, viewer.getWidth());
                arrayCubeShader.setHeight(gl, viewer.getHeight());
                arrayCubeShader.setDepth(gl, transparencyQuarterDepth);
            }
        });
        denseVolumeShaderActionSequence.setShader(arrayCubeShader);
        denseVolumeShaderActionSequence.setApplyMemoryBarrier(true);
    }

    private void setupMeshShader() {
        final ArrayMeshShader arrayMeshShader = new ArrayMeshShader(properties);
        final Integer transparencyQuarterDepth=getTransparencyQuarterDepth();
        arrayMeshShader.setUpdateCallback(new GLDisplayUpdateCallback() {
            @Override
            public void update(GL4 gl) {
                Matrix4 viewMatrix = viewer.getRenderer().getViewMatrix();
                arrayMeshShader.setView(gl, viewMatrix);
                Matrix4 projMatrix = viewer.getRenderer().getProjectionMatrix();
                arrayMeshShader.setProjection(gl, projMatrix);

                arrayMeshShader.setWidth(gl, viewer.getWidth());
                arrayMeshShader.setHeight(gl, viewer.getHeight());
                arrayMeshShader.setDepth(gl, transparencyQuarterDepth);

            }
        });

        meshShaderActionSequence.setShader(arrayMeshShader);
        meshShaderActionSequence.setApplyMemoryBarrier(true);
    }

    public Deque<GL4SimpleActor> getInitQueue() {
        return initQueue;
    }

    public Deque<Integer> getDisposeQueue() {
        return disposeQueue;
    }

    public Deque<String> getMessageQueue() {
        return messageQueue;
    }

    public GL4ShaderActionSequence getDenseVolumeShaderActionSequence() {
        return denseVolumeShaderActionSequence;
    }

    public GL4ShaderActionSequence getMeshShaderActionSequence() {
        return meshShaderActionSequence;
    }

    public int addActorToInitQueue(GL4SimpleActor newActor) {
        synchronized(this) {
            initQueue.add(newActor);

        }
        return newActor.getActorId();
    }

    public void addActor(GL4SimpleActor actor) {
        synchronized (this) {
            if (actor instanceof ArrayCubeActor) {
                denseVolumeShaderActionSequence.getActorSequence().add(actor);
            } else if (actor instanceof ArrayMeshActor) {
                meshShaderActionSequence.getActorSequence().add(actor);
            }
        }
    }

    public void removeActorToDisposeQueue(int actorId) {
        synchronized (this) {
            disposeQueue.add(new Integer(actorId));
        }
    }

    public void removeActor(int actorId, GL4 gl) {
        synchronized (this) {
            GL4SimpleActor toBeRemoved=null;
            for (GL4SimpleActor actor : denseVolumeShaderActionSequence.getActorSequence()) {
                if (actor.getActorId()==actorId) {
                    actor.dispose(gl);
                    toBeRemoved=actor;
                    break;
                }
            }
            if (toBeRemoved!=null) {
                denseVolumeShaderActionSequence.getActorSequence().remove(toBeRemoved);
            } else {
                for (GL4SimpleActor actor : meshShaderActionSequence.getActorSequence()) {
                    if (actor.getActorId()==actorId) {
                        actor.dispose(gl);
                        toBeRemoved=actor;
                        break;
                    }
                }
                if (toBeRemoved!=null) {
                    meshShaderActionSequence.getActorSequence().remove(toBeRemoved);
                }
            }
        }
    }

    public void setDisposeAndClearAllActorsMsg() {
        synchronized (this) {
            messageQueue.add(DISPOSE_AND_CLEAR_ALL_ACTORS_MSG);
        }
    }

    public void disposeAndClearAllActors(GL4 gl) {
        synchronized (this) {
            denseVolumeShaderActionSequence.disposeAndClearActorsOnly(gl);
            meshShaderActionSequence.disposeAndClearActorsOnly(gl);
        }
    }

    public void disposeAndClearAll(GL4 gl) {
        synchronized (this) {
            initQueue.clear();
            denseVolumeShaderActionSequence.dispose(gl);
            meshShaderActionSequence.dispose(gl);
        }
    }

    public void initAll(ArrayTransparencyContext atc, GL4 gl) {
        ArrayCubeShader acs = (ArrayCubeShader)denseVolumeShaderActionSequence.getShader();
        ArrayMeshShader ams = (ArrayMeshShader)meshShaderActionSequence.getShader();
        acs.setTransparencyContext(atc);
        ams.setTransparencyContext(atc);
        try {
            denseVolumeShaderActionSequence.init(gl);
            meshShaderActionSequence.init(gl);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public interface UpdateListener {
        void updateModel();
        void updateRenderer();
    }

    private Collection<UpdateListener> listeners = new ArrayList<>();

    /** This may be useful for situations like the HUD, which retains a reference to
     * the volume model across invocations.  Call this prior to reset.
     */
    public void resetToDefaults() {
    }

    /** Volume update means "must refresh actual data being shown." */
    public void setModelUpdate() {
        Collection<UpdateListener> currentListeners;
        synchronized (this) {
            currentListeners = new ArrayList<>( listeners );
        }
        for ( UpdateListener listener: currentListeners ) {
            listener.updateModel();
        }
    }

    /** Render Update means "must refresh data that affects how the primary data is rendered." */
    public void setRenderUpdate() {
        Collection<UpdateListener> currentListeners;
        synchronized (this) {
            currentListeners = new ArrayList<>( listeners );
        }
        for ( UpdateListener listener: currentListeners ) {
            listener.updateRenderer();
        }
    }

    /** Listener management methods. */
    public synchronized void addUpdateListener( UpdateListener listener ) {
        listeners.add(listener);
    }

    public synchronized void removeUpdateListener( UpdateListener listener ) {
        listeners.remove(listener);
    }

    public synchronized void removeAllListeners() {
        listeners.clear();
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
