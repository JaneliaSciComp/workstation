/*
 * Licensed under the Janelia Farm Research Campus Software Copyright 1.1
 * 
 * Copyright (c) 2014, Howard Hughes Medical Institute, All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *     1. Redistributions of source code must retain the above copyright notice, 
 *        this list of conditions and the following disclaimer.
 *     2. Redistributions in binary form must reproduce the above copyright 
 *        notice, this list of conditions and the following disclaimer in the 
 *        documentation and/or other materials provided with the distribution.
 *     3. Neither the name of the Howard Hughes Medical Institute nor the names 
 *        of its contributors may be used to endorse or promote products derived 
 *        from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, ANY 
 * IMPLIED WARRANTIES OF MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A 
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * REASONABLE ROYALTIES; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.janelia.horta.movie;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.awt.image.BufferedImage;
import org.janelia.console.viewerapi.ComposableObservable;
import org.janelia.console.viewerapi.ObservableInterface;
import org.janelia.geometry3d.Quaternion;
import org.janelia.geometry3d.Rotation;
import org.janelia.geometry3d.Vantage;
import org.janelia.geometry3d.Vector3;
import org.janelia.horta.NeuronTracerTopComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * // TODO: refactor NeuronTracerTopComponent methods into here
 * @author brunsc
 */
public class HortaMovieSource implements MovieSource 
{
    private final NeuronTracerTopComponent horta;
    private static final String VIEWER_STATE_TYPE_STRING = "HortaViewerState";
    private ObservableInterface viewerStateUpdatedObservable = new ComposableObservable();
    
    public HortaMovieSource(NeuronTracerTopComponent horta) {
        this.horta = horta;
    }

    @Override
    public ViewerState getViewerState() {
        return new HortaViewerState(horta);
    }

    @Override
    public void setViewerState(ViewerState state0) 
    {
        HortaViewerState state = (HortaViewerState)state0;
        Vantage vantage = horta.getVantage();
        float [] focus = state.getCameraFocus(); // translation
        vantage.setFocus(focus[0], focus[1], focus[2]);
        vantage.setRotationInGround( // rotation
                new Rotation().setFromQuaternion(state.getCameraRotation()));
        vantage.setSceneUnitsPerViewportHeight( // zoom
            state.getCameraSceneUnitsPerViewportHeight());
        
        vantage.notifyObservers();
        horta.redrawNow();
        
        viewerStateUpdatedObservable.setChanged();
        viewerStateUpdatedObservable.notifyObservers();
    }

    @Override
    public ObservableInterface getViewerStateUpdatedObservable() {
        return viewerStateUpdatedObservable;
    }

    @Override
    public ViewerStateJsonDeserializer getStateDeserializer() {
        return new HortaViewerState.JsonDeserializer();
    }

    @Override
    public String getViewerStateType() {
        return VIEWER_STATE_TYPE_STRING;
    }

    @Override
    public Interpolator<ViewerState> getDefaultInterpolator() {
        return new HortaViewerState.StateInterpolator();
    }

    @Override
    public BufferedImage getRenderedFrame(ViewerState state) 
    {
        setViewerState(state);
        
        // TODO: wait for tile load

        // We need to update the display immediately, not on the next monitor refresh.
        horta.redrawImmediately();
        
        return horta.getScreenShot();
    }

    @Override
    public BufferedImage getRenderedFrame(ViewerState state, int imageWidth, int imageHeight) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean supportsCustomSize() {
        return false;
    }
    
    public static class HortaViewerState implements ViewerState
    {
        // TODO: Expand the set of tracked parameters
        private final float cameraFocusX;
        private final float cameraFocusY;
        private final float cameraFocusZ;
        private final Quaternion cameraRotation;
        private final float cameraZoom;
        
        public HortaViewerState(
                float cameraFocusX, float cameraFocusY, float cameraFocusZ,
                Quaternion cameraRotation,
                float cameraSceneUnitsPerViewportHeight) 
        {
            this.cameraFocusX = cameraFocusX;
            this.cameraFocusY = cameraFocusY;
            this.cameraFocusZ = cameraFocusZ;
            this.cameraRotation = cameraRotation;
            this.cameraZoom = cameraSceneUnitsPerViewportHeight;
        }
        
        public HortaViewerState(NeuronTracerTopComponent horta) 
        {
            Vantage vantage = horta.getVantage();
            float [] focus = vantage.getFocus();
            cameraFocusX = focus[0];
            cameraFocusY = focus[1];
            cameraFocusZ = focus[2];
            cameraRotation = vantage.getRotationInGround().convertRotationToQuaternion();
            cameraZoom = vantage.getSceneUnitsPerViewportHeight();
        }        
        
        public float[] getCameraFocus() {
            return new float[] {cameraFocusX, cameraFocusY, cameraFocusZ};
        }
        
        public Quaternion getCameraRotation() {
            return cameraRotation;
        }
        
        public float getCameraSceneUnitsPerViewportHeight() {
            return cameraZoom;
        }

        @Override
        public String getStateType() {
            return VIEWER_STATE_TYPE_STRING;
        }

        @Override
        public int getStateVersion() {
            return 3;
        }
        
        @Override
        public JsonObject serialize() {
            JsonObject result = new JsonObject();

            HortaViewerState state = this;
            result.addProperty("zoom", state.getCameraSceneUnitsPerViewportHeight());
            float rot[] = state.getCameraRotation().asArray();
            JsonArray quat = new JsonArray();
            for (int i = 0; i < 4; ++i)
                quat.add(new JsonPrimitive(rot[i]));
            result.add("quaternionRotation", quat);
            JsonArray focus = new JsonArray();
            float f[] = state.getCameraFocus();
            for (int i = 0; i < 3; ++i)
                focus.add(new JsonPrimitive(f[i]));
            result.add("focusXyz", focus);

            return result;
        }

        public static class JsonDeserializer implements ViewerStateJsonDeserializer
        {
            @Override
            public ViewerState deserializeJson(JsonObject frame) {
                float zoom = frame.getAsJsonPrimitive("zoom").getAsFloat();
                JsonArray quat = frame.getAsJsonArray("quaternionRotation");
                JsonArray focus = frame.getAsJsonArray("focusXyz");
                float[] f = new float[] {
                    focus.get(0).getAsFloat(),
                    focus.get(1).getAsFloat(),
                    focus.get(2).getAsFloat()};
                Quaternion q = new Quaternion();
                q.set(
                    quat.get(0).getAsFloat(),
                    quat.get(1).getAsFloat(),
                    quat.get(2).getAsFloat(),
                    quat.get(3).getAsFloat());
                HortaViewerState state = new HortaViewerState(
                        f[0], f[1], f[2],
                        q,
                        zoom);
                return state;
            }   
        }
        
        public static class StateInterpolator implements Interpolator<ViewerState>
        {
            private final InterpolatorKernel defaultKernel = 
                    // new LinearInterpolatorKernel();
                    new CatmullRomSplineKernel();
            private final InterpolatorKernel linearKernel = 
                    new LinearInterpolatorKernel();
            private final Interpolator<Vector3> vec3Interpolator = new Vector3Interpolator(defaultKernel);
            private final PrimitiveInterpolator primitiveInterpolator = new PrimitiveInterpolator(defaultKernel);
            private final Interpolator<Quaternion> rotationInterpolator = new PrimitiveInterpolator(defaultKernel);
            private final Logger logger = LoggerFactory.getLogger(this.getClass());

            public StateInterpolator() {
            }

            @Override
            public ViewerState interpolate_equidistant(
                    double ofTheWay, 
                    ViewerState p0arg, ViewerState p1arg, ViewerState p2arg, ViewerState p3arg) 
            {
                HortaViewerState p0 = (HortaViewerState)p0arg;
                HortaViewerState p1 = (HortaViewerState)p1arg;
                HortaViewerState p2 = (HortaViewerState)p2arg;
                HortaViewerState p3 = (HortaViewerState)p3arg;

                Vector3 focus = vec3Interpolator.interpolate_equidistant(ofTheWay, 
                        new Vector3(p0.getCameraFocus()), 
                        new Vector3(p1.getCameraFocus()), 
                        new Vector3(p2.getCameraFocus()), 
                        new Vector3(p3.getCameraFocus()));

                Quaternion rotation = rotationInterpolator.interpolate_equidistant(
                        ofTheWay,
                        p0.getCameraRotation(), 
                        p1.getCameraRotation(), 
                        p2.getCameraRotation(), 
                        p3.getCameraRotation()
                );

                float zoom = primitiveInterpolator.interpolate_equidistant(
                        ofTheWay,
                        p0.getCameraSceneUnitsPerViewportHeight(),
                        p1.getCameraSceneUnitsPerViewportHeight(),
                        p2.getCameraSceneUnitsPerViewportHeight(),
                        p3.getCameraSceneUnitsPerViewportHeight());

                HortaViewerState result = new HortaViewerState(
                        focus.getX(), focus.getY(), focus.getZ(),
                        rotation,
                        zoom
                );

                return result;
            }

            @Override
            public ViewerState interpolate(
                    double ofTheWay, 
                    ViewerState p0arg, ViewerState p1arg, ViewerState p2arg, ViewerState p3arg, 
                    double t0, double t1, double t2, double t3) 
            {
                HortaViewerState p0 = (HortaViewerState)p0arg;
                HortaViewerState p1 = (HortaViewerState)p1arg;
                HortaViewerState p2 = (HortaViewerState)p2arg;
                HortaViewerState p3 = (HortaViewerState)p3arg;
                
                Vector3 focus = vec3Interpolator.interpolate(ofTheWay, 
                        new Vector3(p0.getCameraFocus()), 
                        new Vector3(p1.getCameraFocus()), 
                        new Vector3(p2.getCameraFocus()), 
                        new Vector3(p3.getCameraFocus()), 
                        t0, t1, t2, t3);

                Quaternion rotation = rotationInterpolator.interpolate(
                        ofTheWay,
                        p0.getCameraRotation(), 
                        p1.getCameraRotation(), 
                        p2.getCameraRotation(), 
                        p3.getCameraRotation(), 
                        t0, t1, t2, t3
                );

                float zoom = unpackZoom(primitiveInterpolator.interpolate(
                        ofTheWay,
                        packZoom(p0.getCameraSceneUnitsPerViewportHeight()),
                        packZoom(p1.getCameraSceneUnitsPerViewportHeight()),
                        packZoom(p2.getCameraSceneUnitsPerViewportHeight()),
                        packZoom(p3.getCameraSceneUnitsPerViewportHeight()), 
                        t0, t1, t2, t3));

                // logger.info("ofTheWay = "+ofTheWay);
                // logger.info("zoom = "+zoom);

                HortaViewerState result = new HortaViewerState(
                        focus.getX(), focus.getY(), focus.getZ(),
                        rotation,
                        zoom
                );

                return result;
            }

            // Transform zoom so log(zoom) gets interpolated, not linear zoom
            private double packZoom(float zoom) {
                return Math.log(zoom);
            }

            private float unpackZoom(double logZoom) {
                return (float)Math.exp(logZoom);
            }
        }
    }
    
}
