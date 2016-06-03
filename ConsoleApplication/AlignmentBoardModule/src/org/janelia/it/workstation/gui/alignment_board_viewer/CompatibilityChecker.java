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
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.ReverseReference;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentContext;
import org.janelia.it.jacs.model.domain.sample.Image;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.workstation.gui.alignment_board.AlignmentBoardContext;
import org.janelia.it.workstation.gui.alignment_board_viewer.creation.DomainHelper;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use this to check, in standard/shared-code way, whether objects are compatible
 * with an alignment board or its context.
 *
 * @author fosterl
 */
public class CompatibilityChecker {
    private Logger log = LoggerFactory.getLogger(CompatibilityChecker.class);
    public int getCompatibleFragmentCount(final AlignmentBoardContext abContext, List<DomainObject> itemList) throws UnsupportedFlavorException, IOException, Exception {
        DomainHelper domainHelper = new DomainHelper();
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
                typeIsRef = domainObject instanceof Image && domainObject.getName().startsWith("Reference");
                if (typeIsFragment || typeIsRef) {
                    NeuronFragment fragment = (NeuronFragment) domainObject;
                    sample = domainHelper.getSampleForNeuron(fragment);
                    if (sample == null) {
                        fragmentCount = 0;
                        break;
                    } else {
                        boolean compatible = isSampleCompatibleThrowsEx(standardContext, sample);
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
                        if (compatible) {
                            fragmentCount++;
                        }
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
     * Check if this sample has a context compatible with the 'one of momentum'.  Trap any exceptions.
     */
    public boolean isSampleCompatible(AlignmentContext standardContext, Sample sample) {
        try {
            return isSampleCompatibleThrowsEx(standardContext, sample);
        } catch (Exception ex) {
            SessionMgr.getSessionMgr().handleException(ex);
            return false;
        }
    }

    /**
     * Check if this sample has a context compatible with the 'one of momentum'.
     */
    private boolean isSampleCompatibleThrowsEx(AlignmentContext standardContext, Sample sample) throws Exception {
        boolean rtnVal;
        boolean foundMatch = false;
        DomainHelper domainHelper = new DomainHelper();
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
    
}
