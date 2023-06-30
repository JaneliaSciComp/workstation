
package org.janelia.horta;

import org.janelia.horta.actors.OmeZarrVolumeActor;
import org.janelia.horta.blocks.*;
import org.janelia.horta.render.NeuronMPRenderer;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;
import org.janelia.scenewindow.SceneWindow;
import org.janelia.geometry.util.PerformanceTimer;
import org.janelia.geometry3d.PerspectiveCamera;
import org.janelia.geometry3d.Vantage;
import org.janelia.geometry3d.Vector3;
import org.janelia.gltools.GL3Actor;
import org.janelia.horta.actors.TetVolumeActor;
import org.janelia.horta.volume.BrickActor;
import org.janelia.horta.volume.BrickInfo;
import org.janelia.horta.volume.BrickInfoSet;
import org.janelia.horta.volume.StaticVolumeBrickSource;
import org.openide.awt.StatusDisplayer;

/**
 * Helper to Neuron tracing viewer. Carries out load.
 *
 * @author fosterl
 */
public class NeuronTraceLoader {

    private final NeuronTracerTopComponent nttc;
    private final NeuronMPRenderer neuronMPRenderer;
    private final SceneWindow sceneWindow;
    private int defaultColorChannel = 0;

    public NeuronTraceLoader(
            NeuronTracerTopComponent nttc,
            NeuronMPRenderer neuronMPRenderer,
            SceneWindow sceneWindow) {
        this.nttc = nttc;
        this.neuronMPRenderer = neuronMPRenderer;
        this.sceneWindow = sceneWindow;
    }

    /**
     * Animates to next point in 3D space TODO - run this in another thread
     *
     * @param xyz
     * @param vantage
     */
    boolean animateToFocusXyz(Vector3 xyz, Vantage vantage, int milliseconds) {
        // Disable auto loading during move
        boolean wasCacheEnabled = nttc.doesUpdateVolumeCache();
        nttc.setUpdateVolumeCache(false);

        Vector3 startPos = new Vector3(vantage.getFocusPosition());
        Vector3 endPos = new Vector3(xyz);
        long startTime = System.nanoTime();
        long targetTime = milliseconds * 1000000;
        final int stepCount = 40;
        boolean didMove = false;
        for (int s = 0; s < stepCount - 1; ++s) {
            // skip frames to match expected time
            float alpha = s / (float) (stepCount - 1);
            double deltaTime = (System.nanoTime() - startTime) / 1e6;
            double desiredTime = (alpha * targetTime) / 1e6;
            // logger.info("Elapsed = "+deltaTime+" ms; desired = "+desiredTime+" ms");
            if (deltaTime > desiredTime) {
                continue; // skip this frame
            }
            Vector3 a = new Vector3(startPos).multiplyScalar(1.0f - alpha);
            Vector3 b = new Vector3(endPos).multiplyScalar(alpha);
            a = a.add(b);
            if (vantage.setFocusPosition(a)) {
                didMove = true;
                vantage.notifyObservers();
                sceneWindow.getGLAutoDrawable().display();
            }
        }
        // never skip the final frame
        if (vantage.setFocusPosition(endPos)) {
            didMove = true;
        }
        if (didMove) {
            vantage.notifyObservers();
            sceneWindow.getGLAutoDrawable().display();
        }

        nttc.setUpdateVolumeCache(wasCacheEnabled);
        return didMove;
    }

    BrickInfo loadTileAtCurrentFocus(StaticVolumeBrickSource volumeSource) throws IOException {
        return loadTileAtCurrentFocus(volumeSource, defaultColorChannel);
    }

    public static BrickInfoSet getBricksForCameraResolution(StaticVolumeBrickSource volumeSource, PerspectiveCamera camera) {
        double screenPixelResolution = camera.getVantage().getSceneUnitsPerViewportHeight()
                / camera.getViewport().getHeightPixels();
        double minDist = Double.MAX_VALUE;
        Double bestRes = null;
        for (Double res : volumeSource.getAvailableResolutions()) {
            double dist = Math.abs(Math.log(res) - Math.log(screenPixelResolution));
            if (dist < minDist) {
                bestRes = res;
                minDist = dist;
            }
        }
        Double brickResolution = bestRes;
        assert brickResolution != null : "No best-resolution found.  Volume Source=" + volumeSource;

        BrickInfoSet brickInfoSet = volumeSource.getAllBrickInfoForResolution(brickResolution);
        return brickInfoSet;
    }

    /**
     * Helper method toward automatic tile loading
     */
    BrickInfo loadTileAtCurrentFocus(StaticVolumeBrickSource volumeSource, int colorChannel) throws IOException {
        PerformanceTimer timer = new PerformanceTimer();

        // Remember most recently loaded color channel for next time
        defaultColorChannel = colorChannel;

        PerspectiveCamera pCam = (PerspectiveCamera) sceneWindow.getCamera();
        BrickInfoSet brickInfoSet = getBricksForCameraResolution(volumeSource, pCam);

        BrickInfo brickInfo = brickInfoSet.getBestContainingBrick(pCam.getVantage().getFocusPosition());

        // Check for existing brick already loaded here
        BrainTileInfo brainTileInfo = (BrainTileInfo) brickInfo;
        boolean tileAlreadyLoaded = false;
        for (GL3Actor actor : neuronMPRenderer.getVolumeActors()) {
            if (!(actor instanceof BrickActor)) {
                continue;
            }
            BrickActor ba = (BrickActor) actor;
            if (ba.getBrainTile().isSameBrick(brainTileInfo)
                    && (colorChannel == brainTileInfo.getColorChannelIndex())) // reload if color changed
            {
                tileAlreadyLoaded = true;
                break;
            }
        }

        if ((!tileAlreadyLoaded) && (!nttc.doesUpdateVolumeCache())) {
            GL3Actor boxMesh = nttc.createBrickActor((BrainTileInfo) brickInfo, colorChannel);

            StatusDisplayer.getDefault().setStatusText(
                    "One TIFF file loaded and processed in "
                    + String.format("%1$,.2f", timer.reportMsAndRestart() / 1000.0)
                    + " seconds."
            );

            nttc.registerLoneDisplayedTile((BrickActor) boxMesh);

            // Clear, so only one tiles is shown at a time (two tiles are in memory during transition)
            neuronMPRenderer.clearVolumeActors();
            neuronMPRenderer.addVolumeActor(boxMesh);
        }

        return brickInfo;
    }

    void loadKtxTileAtLocation(KtxOctreeBlockTileSource ktxSource, Vector3 location, final boolean loadPersistent)
            throws IOException {
        if (ktxSource == null) {
            return;
        }
        KtxOctreeBlockTileKey centerKey = ktxSource.getBlockKeyAt(location, ktxSource.getMaximumResolution());
        if (centerKey == null) {
            return;
        }

        if (loadPersistent) {
            final KtxBlockLoadRunner loader = new KtxBlockLoadRunner(ktxSource, centerKey);
            loader.addObserver(new Observer() {
                @Override
                public void update(Observable o, Object arg) {
                    if (loader.state != KtxBlockLoadRunner.State.LOADED) {
                        return;
                    }
                    TetVolumeActor parentActor = TetVolumeActor.getInstance();
                    parentActor.addPersistentBlock(loader.blockActor);
                    if (!neuronMPRenderer.containsVolumeActor(parentActor)) { // just add singleton actor once...
                        parentActor.setBrightnessModel(neuronMPRenderer.getBrightnessModel());
                        neuronMPRenderer.addVolumeActor(parentActor);
                    }
                    neuronMPRenderer.setIntensityBufferDirty();
                    nttc.redrawNow();
                }
            });
            loader.run();
        } else {
            TetVolumeActor parentActor = TetVolumeActor.getInstance();
            if (!neuronMPRenderer.containsVolumeActor(parentActor)) { // just add singleton actor once...
                parentActor.setBrightnessModel(neuronMPRenderer.getBrightnessModel());
                neuronMPRenderer.addVolumeActor(parentActor);
            }
            parentActor.addTransientBlock(centerKey);
        }
    }

    void loadOmeZarrTileAtLocation(OmeZarrBlockTileSource omeZarrSource, Vector3 location, final boolean loadPersistent) {
        if (omeZarrSource == null) {
            return;
        }
        OmeZarrBlockTileKey centerKey = omeZarrSource.getBlockKeyAt(location, omeZarrSource.getMaximumResolution());
        if (centerKey == null) {
            return;
        }

        if (loadPersistent) {
            final OmeZarrBlockLoadRunner loader = new OmeZarrBlockLoadRunner(omeZarrSource, centerKey);
            loader.addObserver(new Observer() {
                @Override
                public void update(Observable o, Object arg) {
                    if (loader.state != OmeZarrBlockLoadRunner.State.LOADED) {
                        return;
                    }
                    OmeZarrVolumeActor parentActor = OmeZarrVolumeActor.getInstance();
                    parentActor.addPersistentBlock(loader.blockActor);
                    if (!neuronMPRenderer.containsVolumeActor(parentActor)) { // just add singleton actor once...
                        parentActor.setBrightnessModel(neuronMPRenderer.getBrightnessModel());
                        neuronMPRenderer.addVolumeActor(parentActor);
                    }
                    neuronMPRenderer.setIntensityBufferDirty();
                    nttc.redrawNow();
                }
            });
            loader.run();
        } else {
            OmeZarrVolumeActor parentActor = OmeZarrVolumeActor.getInstance();
            if (!neuronMPRenderer.containsVolumeActor(parentActor)) { // just add singleton actor once...
                parentActor.setBrightnessModel(neuronMPRenderer.getBrightnessModel());
                neuronMPRenderer.addVolumeActor(parentActor);
            }
            parentActor.addTransientBlock(centerKey);
        }
    }

    void loadPersistentKtxTileAtCurrentFocus(KtxOctreeBlockTileSource ktxSource)
            throws IOException {
        Vector3 focus = sceneWindow.getCamera().getVantage().getFocusPosition();
        loadKtxTileAtLocation(ktxSource, focus, true);
    }

    void loadTransientKtxTileAtCurrentFocus(KtxOctreeBlockTileSource ktxSource)
            throws IOException {
        Vector3 focus = sceneWindow.getCamera().getVantage().getFocusPosition();
        loadKtxTileAtLocation(ktxSource, focus, false);
    }

    void loadPersistentOmeZarrTileAtCurrentFocus(OmeZarrBlockTileSource omeZarrSource)
            throws IOException {
        Vector3 focus = sceneWindow.getCamera().getVantage().getFocusPosition();
        loadOmeZarrTileAtLocation(omeZarrSource, focus, true);
    }

    void loadTransientOmeZarrTileAtCurrentFocus(OmeZarrBlockTileSource omeZarrSource)
            throws IOException {
        Vector3 focus = sceneWindow.getCamera().getVantage().getFocusPosition();
        loadOmeZarrTileAtLocation(omeZarrSource, focus, false);
    }
}
