package org.janelia.horta.neuronvbo;

import java.util.Collection;
import java.util.Iterator;
import javax.media.opengl.GL3;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Object3d;
import org.janelia.gltools.GL3Actor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Actor for testing correctness of new NeuronVbo structure for higher 
 * performance rendering of neuron models using quadric imposters.
 * @author brunsc
 */
public class NeuronVboActor 
        implements GL3Actor, Iterable<NeuronModel>
{
    private final NeuronVboPool vboPool = new NeuronVboPool();
    private boolean bDoHideAllNeurons = false; // for short user-oriented all-neuron toggling
    private boolean bIsVisible = true; // for computational show/hide tricks
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    public NeuronVboActor() 
    {}
    
    public void addNeuron(NeuronModel neuron) {
        if (contains(neuron)) 
            return;
        vboPool.add(neuron);
    }

    public void removeNeuron(NeuronModel neuron) {
        vboPool.remove(neuron);
    }
    
    public boolean contains(NeuronModel neuron) {
        return vboPool.contains(neuron);
    }

    public boolean isHideAll() {
        return bDoHideAllNeurons;
    }

    // Returns true if status changed and there exist hideable items;
    // i.e. if calling this method might have made a difference
    public boolean setHideAll(boolean bDoHideAllNeurons) {
        if (bDoHideAllNeurons == this.bDoHideAllNeurons)
            return false;
        this.bDoHideAllNeurons = bDoHideAllNeurons;
        return ! isEmpty();
    }
    
    public boolean isEmpty() {
        return vboPool.isEmpty();
    }
    
    @Override
    public void display(GL3 gl, AbstractCamera camera, Matrix4 parentModelViewMatrix) {
        if (bDoHideAllNeurons)
            return;
        if (! bIsVisible)
            return;
        vboPool.display(gl, camera);
    }

    @Override
    public Object3d addChild(Object3d child) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<? extends Object3d> getChildren() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getName() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Matrix4 getTransformInParent() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Matrix4 getTransformInWorld() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isVisible() {
        return bIsVisible;
    }

    @Override
    public Object3d setName(String name) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object3d setVisible(boolean isVisible) {
        bIsVisible = isVisible;
        return this;
    }

    @Override
    public Object3d getParent() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object3d setParent(Object3d parent) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void dispose(GL3 gl) {
        log.info("disposing neurons");
        vboPool.dispose(gl);
    }

    @Override
    public void init(GL3 gl) {
        log.info("initializing neurons");
        vboPool.init(gl);
    }

    @Override
    public Iterator<NeuronModel> iterator() {
        return vboPool.iterator();
    }

    public void checkForChanges() {
        vboPool.checkForChanges();
    }
    
    public void checkForChanges(NeuronModel neuron) {
        vboPool.checkForChanges(neuron);
    }
    
}
