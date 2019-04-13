
package org.janelia.gltools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Observer;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.media.opengl.GL3;
import org.janelia.console.viewerapi.ComposableObservable;
import org.janelia.console.viewerapi.ObservableInterface;
import org.janelia.geometry3d.AbstractCamera;

/**
 * One step of a multi-pass rendering pipeline.
 * Add GL3Actors to get the desired shaders.
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class RenderPass // extends BasicGL3Actor
implements GL3Resource
{
    private boolean isDirty = true;
    private boolean cacheResults = false;
    private final List<RenderPass> dependencies = new ArrayList<>();
    private final Collection<RenderPass> children = new ConcurrentLinkedQueue<>();
    protected final List<RenderTarget> renderTargets = new ArrayList<>();
    protected final Framebuffer framebuffer;
    // Using synchronizedList to avoid ConcurrentModificationException in renderScene() JW-27392
    protected final List<GL3Actor> actors = Collections.synchronizedList(new ArrayList<GL3Actor>());
    private final ComposableObservable actorChangeObservable = new ComposableObservable();

    // Queue old resources for deletion
    private final List<GL3Resource> obsoleteResources = new ArrayList<>();


    public RenderPass(Framebuffer framebuffer) {
        this.framebuffer = framebuffer;
    }
    
    public void addRenderTarget(RenderTarget target) {
        renderTargets.add(target);
    }

    public void addDependency(RenderPass pass) {
        dependencies.add(pass);
    }
    
    public void addChild(RenderPass pass) {
        children.add(pass);
    }
    
    protected void renderScene(GL3 gl, AbstractCamera camera) {
        for (RenderPass pass : children) {
            renderScene(gl, camera);
        }
        for (GL3Actor actor : actors)
            if (actor.isVisible())
                actor.display(gl, camera, null);
    }
    
    public void display(GL3 gl, AbstractCamera camera) {
        // Clean up old junk while we have an active context
        for (GL3Resource deadActor : obsoleteResources) {
            deadActor.dispose(gl);
        }
        obsoleteResources.clear();
        
        if (! needsRerun())
            return;
        
        for (RenderPass pass : children)
            pass.display(gl, camera);

        if (actors.isEmpty())
            return;

        if ( (framebuffer != null) && (renderTargets.size() > 0) && (framebuffer.bind(gl)) ) 
        {
            // Don't apply anaglyph colormask during off-screen rendering...
            // TODO - avoid this hack
            byte[] savedColorMask = new byte[] {(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff};
            gl.glGetBooleanv(GL3.GL_COLOR_WRITEMASK, savedColorMask, 0);
            gl.glColorMask(true, true, true, true);
                
            renderScene(gl, camera);
            framebuffer.unbind(gl);
            if (isCacheResults())
                setNeedsRerun(false);

            gl.glColorMask(
                    savedColorMask[0] != 0,
                    savedColorMask[1] != 0,
                    savedColorMask[2] != 0,
                    savedColorMask[3] != 0);
        }
        else {
            // Render to screen if no framebuffer configured
            renderScene(gl, camera);
        }
    }

    public boolean needsRerun() {
        if (! isCacheResults())
            return true;
        if (isDirty) 
            return true;
        for (RenderPass d : dependencies) {
            if (d.needsRerun()) return true;
        }
        return false;
    }

    public boolean isCacheResults() {
        return cacheResults;
    }

    public void setCacheResults(boolean cacheResults) {
        this.cacheResults = cacheResults;
    }
    
    public RenderPass setNeedsRerun(boolean doesNeed) {
        this.isDirty = doesNeed;
        return this;
    }

    public void addActor(GL3Actor actor)
    {
        actors.add(actor);
        actorChangeObservable.setChanged();
        actorChangeObservable.notifyObservers();
    }
    
    public boolean containsActor(GL3Actor actor) {
        return actors.contains(actor);
    }
    
    public void removeActor(GL3Actor actor) {
        if (actors.remove(actor)) {
            obsoleteResources.add(actor);
            actorChangeObservable.setChanged();  
            actorChangeObservable.notifyObservers();
        }
    }
    
    public void clearActors() {
        // Save a list of old actors, so their resources can be reclaimed
        for (GL3Resource oldActor : actors) {
            obsoleteResources.add(oldActor);
        }
        actors.clear();
        actorChangeObservable.setChanged();
        actorChangeObservable.notifyObservers();
    }

    @Override
    public void dispose(GL3 gl)
    {
        for (GL3Actor actor : actors)
            actor.dispose(gl);
        // Clean up old junk while we have an active context
        for (GL3Resource deadActor : obsoleteResources) {
            deadActor.dispose(gl);
        }
        obsoleteResources.clear();
        for (RenderPass pass : children)
            pass.dispose(gl);
    }

    @Override
    public void init(GL3 gl)
    {
        for (RenderPass pass : children)
            pass.init(gl);
        for (GL3Actor actor : actors)
            actor.init(gl);
    }

    public Iterable<GL3Actor> getActors() {
        return actors;
    }
    
    public ObservableInterface getActorChangeObservable() {
        return actorChangeObservable;
    }
}
