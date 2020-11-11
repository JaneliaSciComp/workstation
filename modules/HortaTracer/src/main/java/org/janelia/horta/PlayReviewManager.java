package org.janelia.horta;

import com.jogamp.opengl.util.FPSAnimator;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import org.janelia.geometry3d.Quaternion;
import org.janelia.geometry3d.Rotation;
import org.janelia.geometry3d.Vantage;
import org.janelia.geometry3d.Vector3;
import org.janelia.horta.camera.CatmullRomSplineKernel;
import org.janelia.horta.camera.Interpolator;
import org.janelia.horta.camera.PrimitiveInterpolator;
import org.janelia.horta.camera.Vector3Interpolator;
import org.janelia.workstation.controller.model.TmViewState;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.scenewindow.SceneWindow;

/**
 *
 * @author schauderd
 * all the functionality for managing playthroughs in Horta
 */
public class PlayReviewManager {
    private PlayState playState;
    private boolean pausePlayback;
    private SceneWindow sceneWindow;
    private FPSAnimator fpsAnimator;  
    private NeuronTraceLoader loader;
    private NeuronTracerTopComponent neuronTracer;
    private boolean autoRotation;
    private int fps;
    private int stepScale;
    
    public enum PlayDirection {
        FORWARD, REVERSE
    };
    private int DEFAULT_REVIEW_PLAYBACK_INTERVAL = 20;

    PlayReviewManager(SceneWindow sceneWindow, NeuronTracerTopComponent tracer, NeuronTraceLoader loader) {
        this.sceneWindow = sceneWindow;        
        this.neuronTracer = tracer;
        this.loader = loader;
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

    void reviewPoints(List<TmViewState> locationList, final boolean autoRotation, final int speed, final int stepScale) {
        clearPlayState();          
        playState.setPlayList(locationList);
        setPausePlayback(false);
        this.fps = speed;
        this.fpsAnimator.setFPS(speed);
        this.stepScale = stepScale;
        this.autoRotation = autoRotation;
        SimpleWorker scrollWorker = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                fpsAnimator.start();
                TmViewState sampleLocation = locationList.get(0);
                Quaternion q = new Quaternion();
                float[] quaternionRotation =  sampleLocation.getCameraRotation();
                if (quaternionRotation != null) {
                    q.set(quaternionRotation[0], quaternionRotation[1], quaternionRotation[2], quaternionRotation[3]);
                }
                ViewLoader acceptor = new ViewLoader(
                        loader, neuronTracer, sceneWindow
                );

                // Set up a more flexible scheme for animations
                //acceptor.loadView(sampleLocation);
                Vantage vantage = sceneWindow.getVantage();
                vantage.setRotationInGround(new Rotation().setFromQuaternion(q));
                Thread.sleep(500);

                for (int i = 1; i < locationList.size(); i++) {
                    sampleLocation = locationList.get(i);

                    q = new Quaternion();
                    quaternionRotation = sampleLocation.getCameraRotation();
                    if (quaternionRotation != null) {
                        q.set(quaternionRotation[0], quaternionRotation[1], quaternionRotation[2], quaternionRotation[3]);
                    }
                    acceptor = new ViewLoader(
                            loader, neuronTracer, sceneWindow
                    );

                    // figure out number of steps
                    vantage = sceneWindow.getVantage();
                    float[] startLocation = vantage.getFocus();
                    double distance = Math.sqrt(Math.pow(sampleLocation.getCameraFocusX() - startLocation[0], 2)
                            + Math.pow(sampleLocation.getCameraFocusY() - startLocation[1], 2)
                            + Math.pow(sampleLocation.getCameraFocusZ() - startLocation[2], 2));
                    // # of steps is 1 per uM
                    int steps = (int) Math.round(distance);
                    if (steps < 1) {
                        steps = 1;
                    }
                    steps = steps * stepScale;
                    boolean interrupt = false;
                    animateToLocationWithRotation(q, sampleLocation, steps, null);
                    if (interrupt) {
                        playState.setCurrentNode(i);
                        break;
                    }
                }
                fpsAnimator.stop();
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

    void resumePlaythrough(final PlayDirection direction) {
        if (playState.getPlayList()==null)
            return;
        setPausePlayback(false);
        SimpleWorker scrollWorker = new SimpleWorker() {
            @Override
            protected void doStuff() throws Exception {
                int startNode = playState.getCurrentNode();
                sceneWindow.setControlsVisibility(true);
                List<TmViewState> locationList = playState.getPlayList();
                TmViewState sampleLocation;
                fpsAnimator.setFPS(playState.getFps());
                fpsAnimator.start();
                boolean interrupt;
                if (direction==PlayDirection.REVERSE) {
                    startNode--;
                    interrupt = animateToNextPoint(locationList.get(startNode), null);
                } else {
                    interrupt = animateToNextPoint(locationList.get(startNode), null);
                }

                if (interrupt) {
                    playState.setCurrentNode(startNode);
                } else {

                    switch (direction) {
                        case FORWARD:
                            if (startNode > locationList.size()) {
                                startNode = locationList.size() - 1;
                            }
                            for (int i = startNode + 1; i < locationList.size(); i++) {
                                sampleLocation = locationList.get(i);
                                interrupt = animateToNextPoint(sampleLocation, null);
                                if (interrupt) {
                                    playState.setCurrentNode(i);
                                    break;
                                }
                            }
                            break;
                        case REVERSE:
                            if (startNode < 1) {
                                startNode = 1;
                            }
                            for (int i = startNode - 1; i > 0; i--) {
                                sampleLocation = locationList.get(i);
                                interrupt = animateToNextPoint(sampleLocation, null);
                                if (interrupt) {
                                    playState.setCurrentNode(i+1);
                                    break;
                                }
                            }
                            break;
                    }
                }
                fpsAnimator.stop();
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

    private boolean animateToNextPoint(TmViewState sampleLocation, Integer startStep) throws Exception {
        Quaternion q = new Quaternion();

        float[] quaternionRotation = sampleLocation.getCameraRotation();
        if (quaternionRotation != null) {
            q.set(quaternionRotation[0], quaternionRotation[1], quaternionRotation[2], quaternionRotation[3]);
        }
        ViewLoader acceptor = new ViewLoader(
                loader, neuronTracer, sceneWindow
        );

        // figure out number of steps
        Vantage vantage = sceneWindow.getVantage();
        float[] startLocation = vantage.getFocus();
        double distance = Math.sqrt(Math.pow(sampleLocation.getCameraFocusX() - startLocation[0], 2)
                + Math.pow(sampleLocation.getCameraFocusY() - startLocation[1], 2)
                + Math.pow(sampleLocation.getCameraFocusZ() - startLocation[2], 2));
        // # of steps is 1 per uM
        int steps = (int) Math.round(distance);
        if (steps < 1) {
            steps = 1;
        }
        steps = steps * stepScale;
return true;
        //return animateToLocationWithRotation(acceptor, q, sampleLocation, steps, startStep);
    }

    private boolean animateToLocationWithRotation(Quaternion endRotation, TmViewState endLocation, int steps, Integer startStep) throws Exception {
        Vantage vantage = sceneWindow.getVantage();
        CatmullRomSplineKernel splineKernel = new CatmullRomSplineKernel();
        Interpolator<Vector3> vec3Interpolator = new Vector3Interpolator(splineKernel);
        Interpolator<Quaternion> rotationInterpolator = new PrimitiveInterpolator(splineKernel);
        double stepSize = 1.0 / (float) steps;

        double zoom = endLocation.getZoomLevel();
        if (zoom > 0) {
            vantage.setSceneUnitsPerViewportHeight((float) zoom);
            vantage.setDefaultSceneUnitsPerViewportHeight((float) zoom);
        }

        Vector3 startFocus = new Vector3(sceneWindow.getVantage().getFocusPosition().getX(), sceneWindow.getVantage().getFocusPosition().getY(),
                sceneWindow.getVantage().getFocusPosition().getZ());
        sceneWindow.getVantage().getFocusPosition().copy(startFocus);
        Quaternion startRotation = sceneWindow.getVantage().getRotationInGround().convertRotationToQuaternion();

        Vector3 endFocus = new Vector3((float) endLocation.getCameraFocusX(), (float) endLocation.getCameraFocusY(),
                (float) endLocation.getCameraFocusZ());
        double currWay = 0;
        int startIndex = 0;
        if (startStep!=null) {
            startIndex = startStep;
        }
        for (int i = startIndex; i < steps; i++) {
            Thread.sleep(1000*1/fps);
            currWay += stepSize;
            Vector3 iFocus = vec3Interpolator.interpolate_equidistant(currWay,
                    startFocus, startFocus, endFocus, endFocus);
            Quaternion iRotate = rotationInterpolator.interpolate_equidistant(currWay,
                    startRotation, startRotation, endRotation, endRotation);
            vantage.setFocus(iFocus.getX(), iFocus.getY(), iFocus.getZ());
            if (autoRotation)
                vantage.setRotationInGround(new Rotation().setFromQuaternion(iRotate));
            vantage.notifyObservers();
            if (isPausePlayback()) {
                setPausePlayback(false);
                playState.setCurrentStep(i);
                return true;
            }
        }
        return false;
    }
    
    /**
     * @return the pausePlayback
     */
    boolean isPausePlayback() {
        return this.pausePlayback;
    }

    /**
     * @param pause the pausePlayback to set
     */
    void setPausePlayback(boolean pause) {
        this.pausePlayback = pause;
    }

    void updateSpeed(boolean increase) {
        if (increase)
            this.fps++;
        else 
            this.fps--;
        this.fpsAnimator.stop();
        this.fpsAnimator.setFPS(this.fps);
        this.fpsAnimator.start();
    }
    
    void clearPlayState() {
        playState = new PlayState();
        playState.setCurrentStep(0);
        playState.setCurrentNode(0);  
    }
}
