/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.alignment_board_viewer.creation;

import java.util.ArrayList;
import java.util.List;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentContext;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.sample.NeuronSeparation;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SampleAlignmentResult;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This helper class will walk around the domain data, finding various pieces
 * of data required for other operations.
 *
 * @author fosterl
 */
public class DomainHelper {
    private Logger log = LoggerFactory.getLogger(DomainHelper.class);
    public List<AlignmentContext> getAvailableAlignmentContexts(Sample sample) throws Exception {
        List<AlignmentContext> rtnVal = new ArrayList<>();
        if (sample.getObjectives() != null) {
            for (ObjectiveSample os : sample.getObjectiveSamples()) {
                if (! os.hasPipelineRuns()) {
                    continue;
                }
                List<DomainObject> completeList = DomainMgr.getDomainMgr().getModel().getAllDomainObjectsByClass(AlignmentContext.class.getName());
                for (SamplePipelineRun pipelineRun : os.getPipelineRuns()) {
                    for (PipelineResult result : pipelineRun.getResults()) {
                        if (result instanceof SampleAlignmentResult) {
                            SampleAlignmentResult sar = (SampleAlignmentResult)result;
                            String alignmentSpace = sar.getAlignmentSpace();
                            String imageSize = sar.getImageSize();
                            String opticalResolution = sar.getOpticalResolution();
                            
                            // Find out if this one has been "blessed".
                            AlignmentContext ctx = new AlignmentContext();
                            ctx.setAlignmentSpace(alignmentSpace);
                            ctx.setOpticalResolution(opticalResolution);
                            ctx.setImageSize(imageSize);

                            if (completeList.contains(ctx)) {
                                rtnVal.add(ctx);
                            }
                            else {
                                log.warn("Failed to find context {} among existing.  Rejecting.", ctx);
                            }
                        }
                    }
                }
            }
        }
        return rtnVal;
    }
    
    public List<AlignmentContext> getAllAlignmentContexts() throws Exception {
        final DomainModel model = DomainMgr.getDomainMgr().getModel();
        List<DomainObject> completeList = model.getAllDomainObjectsByClass(AlignmentContext.class.getName());
        List<AlignmentContext> returnList = new ArrayList<>();
        for (DomainObject ctx: completeList) {
            returnList.add((AlignmentContext)ctx);
        }
        return returnList;
    }
    
    public Sample getSampleForNeuron(NeuronFragment nf) {
        Reference sampleRef = nf.getSample();
        return (Sample) DomainMgr.getDomainMgr().getModel().getDomainObject(sampleRef);
    }
            
}

//                for (PipelineResult pResult: pipelineRun.getResults()) {
//                    NeuronSeparation ns = pResult.getLatestSeparationResult();
//                    // This will get me the fragments, later as needed.
//                }
