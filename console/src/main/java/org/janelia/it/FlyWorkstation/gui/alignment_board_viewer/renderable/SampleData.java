package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.renderable;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 3/11/13
 * Time: 9:22 AM
 *
 * This represents data to be kept together to represent one sample.
 */
public class SampleData {
    private RenderableBean sample;
    private Collection<RenderableBean> neuronFragments;
    private Collection<RenderableBean> compartments;
    private RenderableBean reference;
    private String signalFile;
    private String labelFile;
    private String referenceFile;

    public RenderableBean getSample() {
        return sample;
    }

    public void setSample(RenderableBean sample) {
        this.sample = sample;
    }

    /**
     * Builds up a collection of all things to be rendered, except the sample itself.
     *
     * @return fragments, compartments, reference.
     */
    public Collection<RenderableBean> getRenderableBeans() {
        Collection<RenderableBean> rtnVal = new ArrayList<RenderableBean>();
        if ( getNeuronFragments() != null )
            rtnVal.addAll( getNeuronFragments() );
        if ( getCompartments() != null )
            rtnVal.addAll( getCompartments() );
        if ( getReference() != null )
            rtnVal.add( getReference() );
        return rtnVal;
    }

    /**
     * These methods handle neuron fragments.  Such renderables represent things with presence in the
     * label file under a specific key, and presence by-coords in the signal file as well.
     */
    public Collection<RenderableBean> getNeuronFragments() {
        return neuronFragments;
    }

    public void setNeuronFragments(Collection<RenderableBean> neuronFragments) {
        this.neuronFragments = neuronFragments;
    }

    public void addNeuronFragment( RenderableBean fragment ) {
        if ( neuronFragments == null ) {
            neuronFragments = new ArrayList<RenderableBean>();
        }
        neuronFragments.add( fragment );
    }

    /**
     * These methods handle compartments.  Such renderables are specified to have their own files which must be
     * treated separately, and will have on-screen presence.
     */
    public Collection<RenderableBean> getCompartments() {
        return compartments;
    }

    public void setCompartments(Collection<RenderableBean> compartments) {
        this.compartments = compartments;
    }

    public void addCompartment( RenderableBean compartment ) {
        if ( this.compartments == null ) {
            compartments = new ArrayList<RenderableBean>();
        }
        compartments.add( compartment );
    }

    /**
     * The "reference" is a special file that has a distinct outline for the whole brain under scrutiny.
     */
    public RenderableBean getReference() {
        return reference;
    }

    public void setReference(RenderableBean reference) {
        this.reference = reference;
    }

    /**
     * The signal, label and reference files are the actual data underlying the neuron fragment,
     * and reference renderables.
     */
    public String getSignalFile() {
        return signalFile;
    }

    public void setSignalFile(String signalFile) {
        this.signalFile = signalFile;
    }

    public String getLabelFile() {
        return labelFile;
    }

    public void setLabelFile(String labelFile) {
        this.labelFile = labelFile;
    }

    public String getReferenceFile() {
        return referenceFile;
    }

    public void setReferenceFile(String referenceFile) {
        this.referenceFile = referenceFile;
    }
}
