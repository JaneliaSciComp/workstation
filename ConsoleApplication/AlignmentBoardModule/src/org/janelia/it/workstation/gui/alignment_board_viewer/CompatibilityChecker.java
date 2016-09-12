/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.alignment_board_viewer;

import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.ReverseReference;
import org.janelia.it.jacs.model.domain.compartments.CompartmentSet;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentContext;
import org.janelia.it.jacs.model.domain.sample.Image;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.workstation.gui.alignment_board.AlignmentBoardContext;
import org.janelia.it.workstation.gui.alignment_board.util.ABReferenceChannel;
import org.janelia.it.workstation.gui.alignment_board_viewer.creation.DomainHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use this to check, in standard/shared-code way, whether objects are compatible
 * with an alignment board or its context.
 *
 * @author fosterl
 */
public class CompatibilityChecker {
    private final Logger log = LoggerFactory.getLogger(CompatibilityChecker.class);
    private final DomainHelper domainHelper = new DomainHelper();
    
    public int getCompatibleFragmentCount(final AlignmentBoardContext abContext, List<DomainObject> itemList) throws UnsupportedFlavorException, IOException, Exception {
        // Need check for alignment context compatibility.
        AlignmentContext standardContext = abContext.getAlignmentContext();
        boolean typeIsFragment;
        boolean typeIsSample;
        boolean typeIsRef;
        Sample sample;
        int fragmentCount = 0;
        for (DomainObject domainObject : itemList) {
            if (abContext.isAcceptedType(domainObject)) {
                typeIsFragment = domainObject instanceof NeuronFragment;
                typeIsSample = domainObject instanceof Sample;
                typeIsRef = domainObject instanceof Image && domainObject.getName().startsWith(ABReferenceChannel.REF_CHANNEL_TYPE_NAME);
                if (typeIsFragment || typeIsRef) {
                    NeuronFragment fragment = (NeuronFragment) domainObject;
                    sample = domainHelper.getSampleForNeuron(fragment);
                    if (sample == null) {
                        fragmentCount = 0;
                        break;
                    } else if (isSampleCompatibleThrowsEx(standardContext, sample)  &&
                               isNeuronSeparationCompatible(standardContext, sample, fragment)) {
                        fragmentCount++;
                    }
                } else if (typeIsSample) {
                    sample = (Sample) domainObject;
                    boolean compatible = isSampleCompatibleThrowsEx(standardContext, sample);
                    if (compatible) {
                        ReverseReference fragmentsRRef = domainHelper.getNeuronRRefForSample(sample, standardContext);
                        if (fragmentsRRef != null) {
                            fragmentCount += fragmentsRRef.getCount();
                            log.info("Sample {} is compatible.", sample.getName());
                        }
                    }
                }
            }
        }
        return fragmentCount;
    }

    /** Equality convenience method, since changing equals() is impractical on domain objects. */
    public boolean isEqual(AlignmentContext contextA, AlignmentContext contextB) {
        return contextA.getImageSize().equals(contextB.getImageSize())  &&
               contextA.getAlignmentSpace().equals(contextB.getAlignmentSpace())  &&               
               contextA.getOpticalResolution().equals(contextB.getOpticalResolution());
    }

    /**
     * Convenience method not requiring the pre-creation of an alignment space.
     * Caution: please get the strings in the right order!
     * 
     * @param contextA the "standard" space
     * @param alignmentSpace same value one might set on an alignment context.
     * @param opticalResolution ditto
     * @param imageSize ditto
     * @return 
     */
    public boolean isEqual(AlignmentContext contextA, String alignmentSpace, String opticalResolution, String imageSize) {
        return contextA.getImageSize().equals(imageSize)
                && contextA.getAlignmentSpace().equals(alignmentSpace)
                && contextA.getOpticalResolution().equals(opticalResolution);
    }

    /**
     * Given fragment and its containing neuron, figure out if it belongs to the
     * established standard context.
     * 
     * @param standardContext must adhere to this.
     * @param sample contains fragment.
     * @param neuronFragment may/may not be compatible.
     * @return T=same context; F=not
     */
    public boolean isFragmentCompatible(AlignmentContext standardContext, Sample sample, NeuronFragment neuronFragment) {
        boolean rtnVal = false;
        if (sample == null) {
            log.warn("No sample ancestor found for neuron fragment " + neuronFragment.getId());
        }
        else if (isSampleCompatible(standardContext, sample)  &&  isNeuronSeparationCompatible(standardContext, sample, neuronFragment)) {
            rtnVal = true;
        }
            
        return rtnVal;
    }
    
    /**
     * Only aligned neuron fragments may be presented in the alignment board.
     * 
     * @param sample may/may not have alignment context.
     * @return T: has the context.  F: not.
     */
    public boolean isAligned(Sample sample) {
        boolean rtnVal = false;
        try {
            List<AlignmentContext> contexts = domainHelper.getAvailableAlignmentContexts(sample);
            if (contexts != null && !contexts.isEmpty()) {
                rtnVal = true;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return rtnVal;
    }
    
    /**
     * Only aligned neuron fragments may be presented in the alignment board.
     * 
     * @param neuronFragment may/may not have alignment context.
     * @return T: has the context.  F: not.
     */
    public boolean isAligned(NeuronFragment neuronFragment) {
        boolean rtnVal = false;
        try {
            Sample sample = domainHelper.getSampleForNeuron(neuronFragment);
            if (sample != null) {
                AlignmentContext neuronFC = domainHelper.getNeuronFragmentAlignmentContext(sample, neuronFragment);
                if (neuronFC != null) {
                    rtnVal = true;
                }
            }
            
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return rtnVal;
    }
    
    /**
     * Check if this sample has a context compatible with the 'one of momentum'.  Trap any exceptions.
     */
    public boolean isSampleCompatible(AlignmentContext standardContext, Sample sample) {
        try {
            return isSampleCompatibleThrowsEx(standardContext, sample);
        } catch (Exception ex) {
            FrameworkImplProvider.handleException(ex);
            return false;
        }
    }

    /**
     * Check if this compartment set has a context compatible with the 'one of momentum'.
     * Trap any exceptions.
     */
    public boolean isCompartmentSetCompatible(AlignmentContext standardContext, CompartmentSet compartmentSet) {
        try {
            return isEqual(standardContext, compartmentSet.getAlignmentSpace(), compartmentSet.getOpticalResolution(), compartmentSet.getImageSize());
        } catch (Exception ex) {
            FrameworkImplProvider.handleException(ex);
            return false;
        }
    }

    /**
     * Check if this sample has a context compatible with the 'one of momentum'.
     */
    private boolean isSampleCompatibleThrowsEx(AlignmentContext standardContext, Sample sample) throws Exception {
        boolean rtnVal;
        boolean foundMatch = false;
        List<AlignmentContext> contexts = domainHelper.getAvailableAlignmentContexts(sample);
        if (contexts.isEmpty()) {
            log.warn("No available contexts in sample {}.", sample.getName());
        }
        Iterator<AlignmentContext> contextIterator = contexts.iterator();

        while (contextIterator.hasNext() && (!foundMatch)) {
            AlignmentContext nextContext = contextIterator.next();
            if (standardContext.getImageSize().equals(nextContext.getImageSize())
                    && standardContext.getAlignmentSpace().equals(nextContext.getAlignmentSpace())
                    && standardContext.getOpticalResolution().equals(nextContext.getOpticalResolution())) {

                foundMatch = true;
            }

        }

        rtnVal = foundMatch;
        return rtnVal;
    }
    
    private boolean isNeuronSeparationCompatible(AlignmentContext standardContext, Sample sample, NeuronFragment fragment) {
        boolean compatible;
        // Must establish that the fragment's separation is
        // also compatible.
        AlignmentContext neuronFC = domainHelper.getNeuronFragmentAlignmentContext(sample, fragment);
        if (neuronFC != null  &&
                neuronFC.getAlignmentSpace().equals(standardContext.getAlignmentSpace())  &&
                neuronFC.getImageSize().equals(standardContext.getImageSize())  &&
                neuronFC.getOpticalResolution().equals(standardContext.getOpticalResolution())) {
            
            compatible = true;
        }
        else {
            compatible = false;
        }
        return compatible;
    }
    
}
