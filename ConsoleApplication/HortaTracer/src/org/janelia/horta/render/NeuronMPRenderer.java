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

package org.janelia.horta.render;

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import org.janelia.console.viewerapi.ObservableInterface;
import org.janelia.console.viewerapi.controller.TransactionManager;
import org.janelia.geometry3d.AbstractCamera;
// import org.janelia.geometry3d.ChannelBrightnessModel;
import org.janelia.gltools.BasicScreenBlitActor;
import org.janelia.gltools.GL3Actor;
import org.janelia.gltools.LightingBlitActor;
import org.janelia.gltools.MultipassRenderer;
import org.janelia.gltools.RemapColorActor;
import org.janelia.gltools.RenderPass;
import org.janelia.gltools.RenderTarget;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.console.viewerapi.model.NeuronSet;
import org.janelia.geometry3d.Matrix4;
import org.janelia.gltools.BasicGL3Actor;
import org.janelia.gltools.ShaderProgram;
import org.janelia.gltools.texture.Texture2d;
import org.janelia.console.viewerapi.model.HortaMetaWorkspace;
import org.janelia.console.viewerapi.model.ImageColorModel;
import org.janelia.geometry3d.PerspectiveCamera;
import org.janelia.gltools.GL3Resource;
import org.janelia.gltools.MeshActor;
import org.janelia.horta.neuronvbo.NeuronVboActor;
import org.openide.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Multi-pass renderer for Horta volumes and neuron models
 * @author Christopher Bruns
 */
public class NeuronMPRenderer
extends MultipassRenderer
{
    private final GLAutoDrawable drawable;
    private final BackgroundRenderPass backgroundRenderPass;
    private final OpaqueRenderPass opaqueRenderPass;
    private final VolumeRenderPass volumeRenderPass;
    
    private final Set<NeuronSet> currentNeuronLists = new HashSet<>();
    private final HortaMetaWorkspace workspace;
    private final Observer neuronListRefresher = new NeuronListRefresher(); // helps with signalling
    private final Observer volumeLayerExpirer = new VolumeLayerExpirer();
    
    private static final Logger log = LoggerFactory.getLogger(NeuronMPRenderer.class);
    
    // private final AllSwcActor allSwcActor = new AllSwcActor();
    private final NeuronVboActor allSwcActor = new NeuronVboActor();
    
    private final Collection<GL3Resource> obsoleteGLResources = new java.util.concurrent.ConcurrentLinkedQueue<>();

    // TODO: obsolete brightness model for ImageColorModel
    // private final ChannelBrightnessModel brightnessModel;
    private final ImageColorModel imageColorModel;
    
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public NeuronMPRenderer(GLAutoDrawable drawable, 
            // final ChannelBrightnessModel brightnessModel, 
            HortaMetaWorkspace workspace, 
            final ImageColorModel imageColorModel) 
    {
        this.drawable = drawable;
        // this.brightnessModel = brightnessModel;
        this.imageColorModel = imageColorModel;
        
        this.workspace = workspace;
        workspace.addObserver(neuronListRefresher);
        
        backgroundRenderPass = new BackgroundRenderPass();
        add(backgroundRenderPass);
        
        // CMB September 2015 begin work on opaque render pass
        opaqueRenderPass = new OpaqueRenderPass(drawable);
        opaqueRenderPass.addActor(allSwcActor);
        add(opaqueRenderPass);
        
        volumeRenderPass = new VolumeRenderPass(drawable);

        // pass depth texture from opaque render pass as input to volume render pass
        // using trivial intermediate render pass
        add(new RenderPass(null) {
            @Override
            public void display(GL3 gl, AbstractCamera camera) {
                volumeRenderPass.setOpaqueDepthTexture(
                        opaqueRenderPass.getFlatDepthTarget());
                super.display(gl, camera);
            }
        });
        
        add(volumeRenderPass);
        
        // 2.5 blit opaque geometry to screen
        add(new RenderPass(null) {
            { // constructor
                addActor(new BasicScreenBlitActor(opaqueRenderPass.getColorTarget()));
            }
        });
        
        // 3) Colormap volume onto screen
        add(new RenderPass(null) { // render to screen
            private GL3Actor lightingActor = new LightingBlitActor(
                    volumeRenderPass.getRgbaTexture()); // for isosurface
            private final GL3Actor colorMapActor = new RemapColorActor(
                    volumeRenderPass.getRgbaTexture(), imageColorModel); // for MIP, occluding
            {
                // addActor(lightingActor); // TODO - use for isosurface
                addActor(colorMapActor); // Use for MIP and occluding
                // lightingActor.setVisible(false);
                // colorMapActor.setVisible(true);
                // addActor(new BasicScreenBlitActor(volumeRenderPass.getIntensityTexture()));
            }
            
            @Override
            protected void renderScene(GL3 gl, AbstractCamera camera) {
                gl.glEnable(GL3.GL_BLEND);
                gl.glBlendFunc(GL3.GL_SRC_ALPHA, GL3.GL_ONE_MINUS_SRC_ALPHA);
                super.renderScene(gl, camera);
            }
        });
        
        setRelativeSlabThickness(0.92f, 1.08f);
    }
    
    public ImageColorModel getBrightnessModel() {
        return imageColorModel;
    }
    
    public void addMeshActor(MeshActor meshActor) {
        opaqueRenderPass.addActor(meshActor);
        setOpaqueBufferDirty();
    }
    
    public void removeMeshActor(MeshActor meshActor) {
        opaqueRenderPass.removeActor(meshActor);
        setOpaqueBufferDirty();
    }
    
    public void addVolumeActor(GL3Actor boxMesh) {
        volumeRenderPass.addActor(boxMesh);
        setIntensityBufferDirty();
    }
    
    public boolean containsVolumeActor(GL3Actor actor) {
        return volumeRenderPass.containsActor(actor);
    }
    
    public void clearVolumeActors() {
        for (GL3Actor actor: volumeRenderPass.getActors()) {
            obsoleteGLResources.add(actor);
        }
        volumeRenderPass.clearActors();
        setIntensityBufferDirty();
    }
    
    @Override
    public void display(GL3 gl, AbstractCamera camera) 
    {
        // Take out the garbage, such as old 3D volume blocks
        Iterator<GL3Resource> iter = obsoleteGLResources.iterator();
        while (iter.hasNext()) {
            GL3Resource resource = iter.next();
            resource.dispose(gl);
            iter.remove();
        }
        
        super.display(gl, camera);
    }

    
    @Override
    public void init(GL3 gl) {
        super.init(gl);
    }
    
    @Override
    public void dispose(GL3 gl) {
        super.dispose(gl);
    }
    
    public double coreIntensityForScreenXy(Point2D xy) {     
        double result;
        // Neurite core tracing intensity is located in the first channel of the coreDepth target
        result = valueForScreenXy(xy, volumeRenderPass.getCoreDepthTexture().getAttachment(), 0);
        // Convert from range 0-1 to range 0-65535
        result *= 65535.0;
        if (result <= 0) {
            // logger.info("Nonpositive intensity = " + result);
            return -1;
        }
        return result;
    }
    
    public double volumeOpacityForScreenXy(Point2D xy)
    {
        float result = -1;
        double intensity = coreIntensityForScreenXy(xy);
        if (intensity == -1) {
            return result;
        }
        if (volumeRenderPass.getFramebuffer() == null) {
            return result;
        }
        RenderTarget coreDepthTarget = volumeRenderPass.getCoreDepthTexture();
        if (coreDepthTarget == null) {
            return result;
        }
        // Opacity is packed together with the relative depth
        double opacity = coreDepthTarget.getIntensity(
                drawable,
                (int) xy.getX(),
                // y convention is opposite between screen and texture buffer
                coreDepthTarget.getHeight() - (int) xy.getY(),
                1); // channel index
        // Truncate fractional part, and rescale from range 0-127 to 0-1
        opacity = ((long)opacity) / 127.0;
        return opacity;
    }

    // Ranges from zNear(returns 0.0) to zFar(returns 1.0)
    private double relativeTransparentDepthOffsetForScreenXy(Point2D xy, AbstractCamera camera) {
        double result = 0;
        double intensity = coreIntensityForScreenXy(xy);
        if (intensity == -1) {
            return result;
        }
        if (volumeRenderPass.getFramebuffer() == null) {
            return result;
        }
        RenderTarget coreDepthTarget = volumeRenderPass.getCoreDepthTexture();
        if (coreDepthTarget == null) {
            return result;
        }
        double relDepth = coreDepthTarget.getIntensity(
                drawable,
                (int) xy.getX(),
                // y convention is opposite between screen and texture buffer
                coreDepthTarget.getHeight() - (int) xy.getY(),
                1); // channel index
        // Remove integer part, which contains opacity, for blending purposes
        long opacityPart = (long)relDepth;
        relDepth = relDepth - opacityPart;
        result = 1.0 - relDepth; // Reverse sense of relative depth to near-small from far-small
        return result;
    }
    
    // TODO: move to volumeRenderpass
    private double opacityForScreenXy(Point2D xy, AbstractCamera camera) {
        double result = valueForScreenXy(xy, volumeRenderPass.getRgbaTexture().getAttachment(), 3);
        if (result <= 0)
            result = 0;
        return result / 255.0; // rescale to range 0-1
    }
    
    private boolean isVisibleOpaqueAtScreenXy(Point2D xy) {
        double od = opaqueRenderPass.rawZDepthForScreenXy(xy, drawable);
        if (od >= 1.0) 
            return false; // far clip value means no geometry there
        double opacity = volumeOpacityForScreenXy(xy);
        // TODO: threshold might need to be tuned
        // 0.5 seems too small, I want to select that vertex!
        final double opacityThreshold = 0.9; // Always use transparent material, if it's dense enough
        if (opacity >= opacityThreshold)
            return false; // transparent geometry is strong here, so no, not well visible
        return true; // I see a neuron model at this spot
    }
    
    private boolean isVisibleTransparentAtScreenXy(Point2D xy) {
        double opacity = volumeOpacityForScreenXy(xy);
        return (opacity > 0);
    }
    
    // Returns signed difference between focusDistance depth, and depth of item at screen point xy
    public double depthOffsetForScreenXy(Point2D xy, AbstractCamera camera) 
    {
        if (isVisibleOpaqueAtScreenXy(xy)) {
            double od = opaqueRenderPass.rawZDepthForScreenXy(xy, drawable);
            // Definitely use opaque geometry for depth at this point
            // TODO - transform to scene units (micrometers)
            double zNear = opaqueRenderPass.getZNear();
            double zFar = opaqueRenderPass.getZFar();
            double zFocus = opaqueRenderPass.getZFocus();
            double zBuf = od;
            // code lifted from VolumeMipFrag.glsl, which wants the same depth information
            double zEye = 2*zFar*zNear / (zFar + zNear - (zFar - zNear)*(2*zBuf - 1));
            return zEye - zFocus;
        }
        else if (isVisibleTransparentAtScreenXy(xy)) {
            double zRel = relativeTransparentDepthOffsetForScreenXy(xy, camera); // range [0,1]
            double focusDistance = ((PerspectiveCamera)camera).getCameraFocusDistance();
            double zNear = getRelativeZNear() * focusDistance;
            double zFar = getRelativeZFar() * focusDistance;
            double zSubject = zNear + zRel * (zFar - zNear);
            return zSubject - focusDistance;
        }
        else { // we have neither opaque nor transparent geometry to calibrate depth
            return 0;
        }
    }

    public final void setRelativeSlabThickness(float zNear, float zFar) {
        opaqueRenderPass.setRelativeSlabThickness(zNear, zFar);
        volumeRenderPass.setRelativeSlabThickness(zNear, zFar);
        volumeRenderPass.setOpaqueDepthTexture(
            opaqueRenderPass.getFlatDepthTarget());
    }
    
    public float getRelativeZNear() {
        return volumeRenderPass.getRelativeZNear();
    }

    public float getRelativeZFar() {
        return volumeRenderPass.getRelativeZFar();
    }

    private void addNeuronReconstruction(NeuronModel neuron) {
        allSwcActor.addNeuron(neuron);
        
        neuron.getVisibilityChangeObservable().addObserver(volumeLayerExpirer);
    }
    
    public void clearNeuronReconstructions() {
        for (NeuronModel neuron : allSwcActor) {
            removeNeuronReconstruction(neuron);
        }
    }
    
    private void removeNeuronReconstruction(NeuronModel neuron) {
        allSwcActor.removeNeuron(neuron);
        neuron.getVisibilityChangeObservable().deleteObserver(volumeLayerExpirer);
    }
    
    private double opaqueDepthForScreenXy(Point2D xy) {
        return opaqueRenderPass.rawZDepthForScreenXy(xy, drawable);
    }
    
    private double valueForScreenXy(Point2D xy, int glAttachment, int channel) {
        int result = -1;
        if (volumeRenderPass.getFramebuffer() == null) {
            return result;
        }
        RenderTarget target = volumeRenderPass.getFramebuffer().getRenderTarget(glAttachment);
        if (target == null) {
            return result;
        }
        double intensity = target.getIntensity(
                drawable,
                (int) Math.round(xy.getX()),
                // y convention is opposite between screen and texture buffer
                target.getHeight() - (int) Math.round(xy.getY()),
                channel); // channel index
        return intensity;
    }
    
    public void setIntensityBufferDirty() {
        for (RenderTarget rt : new RenderTarget[] {
                    volumeRenderPass.getRgbaTexture(),
                    volumeRenderPass.getCoreDepthTexture()}) 
        {
            rt.setDirty(true);
        }
    }
    
    public void setOpaqueBufferDirty() {
        opaqueRenderPass.setBuffersDirty();
    }

    public Iterable<GL3Actor> getVolumeActors()
    {
        return volumeRenderPass.getActors();
    }
    
    public List<MeshActor> getMeshActors()
    {
        List<MeshActor> collection = new ArrayList<>();
        Iterable<GL3Actor> actors = opaqueRenderPass.getActors();
        for (GL3Actor actor: actors) {
            if (actor instanceof MeshActor)
                collection.add ((MeshActor)actor);
        };
        return collection;
    }
    
    public ObservableInterface getMeshObserver () {
        return opaqueRenderPass.getActorChangeObservable();
    }
    
    public void setBackgroundColor(Color topColor, Color bottomColor) {
        backgroundRenderPass.setColor(topColor, bottomColor);
    }

    // Quick method for hiding all neuron models - ulimately attached to "V" keystroke
    // Returns true if status changed and there exist hideable items;
    // i.e. if calling this method might have made a difference
    public boolean setHideAll(boolean doHideAll) {
        boolean result = allSwcActor.setHideAll(doHideAll);
        if (result) {
            setIntensityBufferDirty(); // Now we can see behind the neurons, so repaint
            setOpaqueBufferDirty();
        }
        return result;
    }

    public boolean isNeuronModelAt(Point2D xy)
    {
        return isVisibleOpaqueAtScreenXy(xy);
    }

    public boolean isVolumeDensityAt(Point2D xy)
    {
        return isVisibleTransparentAtScreenXy(xy);
    }

    public void queueObsoleteResource(GL3Resource resource) {
        obsoleteGLResources.add(resource);
    }

    private class VolumeLayerExpirer implements Observer
    {

        @Override
        public void update(Observable o, Object arg)
        {
            setIntensityBufferDirty(); // Now we can see behind the neuron, so repaint
        }
        
    }
    
    public void addNeuronActors(NeuronModel neuron) {
        if (allSwcActor.contains(neuron)) 
            return;
        allSwcActor.addNeuron(neuron);

        neuron.getVisibilityChangeObservable().addObserver(volumeLayerExpirer);
    }
    
    private class NeuronListRefresher implements Observer 
    {

        @Override
        public void update(Observable o, Object arg)
        {
            // if transactions are enabled don't run this till the endTransaction
            TransactionManager tm = TransactionManager.getInstance();
            if (tm.isTransactionStarted()) {
                tm.addObservables(this, o, arg);
                return;
            }
            
            // Update neuron models
            Set<NeuronModel> latestNeurons = new java.util.HashSet<>();
            Set<NeuronSet> latestNeuronLists = new java.util.HashSet<>();
            // 1 - enumerate latest neurons
            
            
            for (NeuronSet neuronList : workspace.getNeuronSets()) {
                latestNeuronLists.add(neuronList);
                latestNeurons.addAll(neuronList);
            }
            // 2 - remove obsolete neurons
            Set<NeuronModel> obsoleteNeurons = new HashSet<>();
            Set<NeuronModel> newNeurons = new HashSet<>();
            
            for (NeuronModel neuron : allSwcActor) {
                if (!latestNeurons.remove(neuron))
                    obsoleteNeurons.add(neuron);
            }
            
            for (NeuronModel neuron : obsoleteNeurons) {
                removeNeuronReconstruction(neuron);
            }
            
            Iterator<NeuronSet> nli = currentNeuronLists.iterator();
            while (nli.hasNext()) {
                NeuronSet set = nli.next();
                if (! latestNeuronLists.contains(set)) {
                    nli.remove();
                    set.getMembershipChangeObservable().deleteObserver(neuronListRefresher);
                }
            }

            // 3 - add new neurons
            
            for (NeuronSet neuronList : workspace.getNeuronSets()) {
                if (currentNeuronLists.add(neuronList)) {
                    neuronList.getMembershipChangeObservable().addObserver(neuronListRefresher);
                }
            }
            for (NeuronModel neuron : latestNeurons) {
                 addNeuronReconstruction(neuron);
            }
            // 4 - double check for changes in neuron size (e.g. after transfer neurite)
            if (arg!=null && arg instanceof List) {
                List neuronsToRefresh = (List)arg;
                for (int i=0; i<neuronsToRefresh.size(); i++) {
                     allSwcActor.checkForChanges((NeuronModel)neuronsToRefresh.get(i));
                }
            } else {
                allSwcActor.checkForChanges();
                // single neuron changes, no need to check everything
            }
        }
    }

}
