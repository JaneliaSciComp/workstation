package org.janelia.horta;

import com.jogamp.opengl.util.FPSAnimator;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import org.janelia.console.viewerapi.BasicSampleLocation;
import org.janelia.console.viewerapi.SampleLocation;
import org.janelia.console.viewerapi.ViewerLocationAcceptor;
import org.janelia.geometry3d.Quaternion;
import org.janelia.geometry3d.Rotation;
import org.janelia.geometry3d.Vantage;
import org.janelia.geometry3d.Vector3;
import org.janelia.horta.camera.CatmullRomSplineKernel;
import org.janelia.horta.camera.Interpolator;
import org.janelia.horta.camera.PrimitiveInterpolator;
import org.janelia.horta.camera.Vector3Interpolator;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.scenewindow.SceneWindow;
import org.openide.util.Exceptions;

/**
 *
 * @author schauderd
 * all the functionality for managing playthroughs in Horta
 */
public class PlayReviewManager {
    private PlayState playState;
    private boolean pausePlayback = false;
    private Vantage vantage;
    private SceneWindow sceneWindow;
    private FPSAnimator fpsAnimator;  
    private NeuronTraceLoader loader;
    private NeuronTracerTopComponent neuronTracer;
    private String currentSource;
    
    enum PlayDirection {
        FORWARD, REVERSE
    };
    private int DEFAULT_REVIEW_PLAYBACK_INTERVAL = 20;

    public PlayReviewManager(SceneWindow sceneWindow, NeuronTracerTopComponent tracer, NeuronTraceLoader loader) {
        this.sceneWindow = sceneWindow;        
        this.neuronTracer = tracer;
        this.loader = loader;
        this.vantage = sceneWindow.getVantage();
        fpsAnimator = new FPSAnimator(sceneWindow.getGLAutoDrawable(), DEFAULT_REVIEW_PLAYBACK_INTERVAL, true);
        this.sceneWindow.addPauseListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pausePlayback = true;
            }
        });
        this.sceneWindow.addPlayForwardListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resumePlaythrough (PlayDirection.FORWARD);
            }
        });        
        this.sceneWindow.addPlayReverseListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resumePlaythrough (PlayDirection.REVERSE);
            }
        });
    }

    public void reviewPoints(final List<SampleLocation> locationList, final String currentSource) {
        if (playState == null) {
            playState = new PlayState();
            playState.setPlayList(locationList);
            playState.setCurrentStep(0);
        }
        this.currentSource = currentSource;
        SimpleWorker scrollWorker = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                try {
                    fpsAnimator.start();
                    SampleLocation sampleLocation = locationList.get(0);
                    Quaternion q = new Quaternion();
                    float[] quaternionRotation = sampleLocation.getRotationAsQuaternion();
                    if (quaternionRotation != null) {
                        q.set(quaternionRotation[0], quaternionRotation[1], quaternionRotation[2], quaternionRotation[3]);
                    }
                    ViewerLocationAcceptor acceptor = new SampleLocationAcceptor(
                            currentSource, loader, neuronTracer, sceneWindow
                    );
                    acceptor.acceptLocation(sampleLocation);
                    Vantage vantage = sceneWindow.getVantage();
                    vantage.setRotationInGround(new Rotation().setFromQuaternion(q));
                    Thread.sleep(DEFAULT_REVIEW_PLAYBACK_INTERVAL);

                    for (int i = 1; i < locationList.size(); i++) {
                        sampleLocation = locationList.get(i);

                        q = new Quaternion();
                        quaternionRotation = sampleLocation.getRotationAsQuaternion();
                        if (quaternionRotation != null) {
                            q.set(quaternionRotation[0], quaternionRotation[1], quaternionRotation[2], quaternionRotation[3]);
                        }
                        acceptor = new SampleLocationAcceptor(
                                currentSource, loader, neuronTracer, sceneWindow
                        );

                        // figure out number of steps
                        vantage = sceneWindow.getVantage();
                        float[] startLocation = vantage.getFocus();
                        double distance = Math.sqrt(Math.pow(sampleLocation.getFocusXUm() - startLocation[0], 2)
                                + Math.pow(sampleLocation.getFocusYUm() - startLocation[1], 2)
                                + Math.pow(sampleLocation.getFocusZUm() - startLocation[2], 2));
                        // # of steps is 1 per uM
                        int steps = (int) Math.round(distance);
                        if (steps < 1) {
                            steps = 1;
                        }

                        animateToLocationWithRotation(acceptor, q, sampleLocation, steps);
                      
                        if (isPausePlayback()) {
                            setPausePlayback(false);
                            playState.setCurrentStep(i);
                            break;
                        }
                    }
                    fpsAnimator.stop();
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                }
            }

            @Override
            protected void hadSuccess() {
            }

            @Override
            protected void hadError(Throwable error) {
            }
        };
        scrollWorker.execute();

    }

    private void resumePlaythrough(final PlayDirection direction) {
        SimpleWorker scrollWorker = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                try {
                    sceneWindow.setControlsVisibility(true);
                    List<SampleLocation> locationList = playState.getPlayList();
                    SampleLocation sampleLocation;
                    Quaternion q;
                    float[] quaternionRotation = new float[4];
                    ViewerLocationAcceptor acceptor;
                    Vantage vantage;
                    fpsAnimator.setFPS(playState.getFps());
                    fpsAnimator.start();
                    switch (direction) {
                        case FORWARD:
                            int startStep = playState.getCurrentStep();
                            for (int i = startStep; i < locationList.size(); i++) {
                                sampleLocation = locationList.get(i);
                                animateToNextPoint(sampleLocation);
                                if (isPausePlayback()) {
                                    setPausePlayback(false);
                                    playState.setCurrentStep(i);
                                    break;
                                }
                            }
                            break;
                        case REVERSE:
                            startStep = playState.getCurrentStep();
                            for (int i = startStep; i > 0; i--) {
                                sampleLocation = locationList.get(i);
                                animateToNextPoint(sampleLocation);
                                if (isPausePlayback()) {
                                    setPausePlayback(false);
                                    playState.setCurrentStep(i);
                                    break;
                                }
                            }
                            break;
                    }
                    fpsAnimator.stop();

                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                }
            }

            @Override
            protected void hadSuccess() {
            }

            @Override
            protected void hadError(Throwable error) {
            }
        };
        scrollWorker.execute();
    }

    private void animateToNextPoint(SampleLocation sampleLocation) throws Exception {
        Quaternion q = new Quaternion();

        float[] quaternionRotation = sampleLocation.getRotationAsQuaternion();
        if (quaternionRotation != null) {
            q.set(quaternionRotation[0], quaternionRotation[1], quaternionRotation[2], quaternionRotation[3]);
        }
        ViewerLocationAcceptor acceptor = new SampleLocationAcceptor(
                currentSource, loader, neuronTracer, sceneWindow
        );

        // figure out number of steps
        Vantage vantage = sceneWindow.getVantage();
        float[] startLocation = vantage.getFocus();
        double distance = Math.sqrt(Math.pow(sampleLocation.getFocusXUm() - startLocation[0], 2)
                + Math.pow(sampleLocation.getFocusYUm() - startLocation[1], 2)
                + Math.pow(sampleLocation.getFocusZUm() - startLocation[2], 2));
        // # of steps is 1 per uM
        int steps = (int) Math.round(distance);
        if (steps < 1) {
            steps = 1;
        }

        animateToLocationWithRotation(acceptor, q, sampleLocation, steps);
    }

    private void animateToLocationWithRotation(ViewerLocationAcceptor acceptor, Quaternion endRotation, SampleLocation endLocation, int steps) throws Exception {
        Vantage vantage = sceneWindow.getVantage();
        CatmullRomSplineKernel splineKernel = new CatmullRomSplineKernel();
        Interpolator<Vector3> vec3Interpolator = new Vector3Interpolator(splineKernel);
        Interpolator<Quaternion> rotationInterpolator = new PrimitiveInterpolator(splineKernel);
        double stepSize = 1.0 / (float) steps;

        double zoom = endLocation.getMicrometersPerWindowHeight();
        if (zoom > 0) {
            vantage.setSceneUnitsPerViewportHeight((float) zoom);
            vantage.setDefaultSceneUnitsPerViewportHeight((float) zoom);
        }

        Vector3 startFocus = new Vector3(sceneWindow.getVantage().getFocusPosition().getX(), sceneWindow.getVantage().getFocusPosition().getY(),
                sceneWindow.getVantage().getFocusPosition().getZ());
        sceneWindow.getVantage().getFocusPosition().copy(startFocus);
        Quaternion startRotation = sceneWindow.getVantage().getRotationInGround().convertRotationToQuaternion();

        Vector3 endFocus = new Vector3((float) endLocation.getFocusXUm(), (float) endLocation.getFocusYUm(),
                (float) endLocation.getFocusZUm());
        double currWay = 0;
        for (int i = 0; i < steps; i++) {
            Thread.sleep(DEFAULT_REVIEW_PLAYBACK_INTERVAL);
            SampleLocation sampleLocation = new BasicSampleLocation();
            currWay += stepSize;
            Vector3 iFocus = vec3Interpolator.interpolate_equidistant(currWay,
                    startFocus, startFocus, endFocus, endFocus);
            Quaternion iRotate = rotationInterpolator.interpolate_equidistant(currWay,
                    startRotation, startRotation, endRotation, endRotation);
            vantage.setFocus(iFocus.getX(), iFocus.getY(), iFocus.getZ());
            vantage.setRotationInGround(new Rotation().setFromQuaternion(iRotate));
            vantage.notifyObservers();
            //sceneWindow.redrawImmediately();
        }
    }
    
    /**
     * @return the pausePlayback
     */
    public boolean isPausePlayback() {
        return pausePlayback;
    }

    /**
     * @param pausePlayback the pausePlayback to set
     */
    public void setPausePlayback(boolean pausePlayback) {
        this.pausePlayback = pausePlayback;
    }
}
