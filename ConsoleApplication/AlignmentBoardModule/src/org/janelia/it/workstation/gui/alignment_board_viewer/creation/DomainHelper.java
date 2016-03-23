/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.alignment_board_viewer.creation;

import java.util.ArrayList;
import java.util.List;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.sample.NeuronSeparation;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SampleAlignmentResult;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.model.domain.AlignmentContext;

/**
 * This helper class will walk around the domain data, finding various pieces
 * of data required for other operations.
 *
 * @author fosterl
 */
public class DomainHelper {
    public List<AlignmentContext> getAvailableAlignmentContexts(Sample sample) throws Exception {
        List<AlignmentContext> rtnVal = new ArrayList<>();
        if (sample.getObjectives() != null) {
            for (String objKey : sample.getObjectives().keySet()) {
                ObjectiveSample os = sample.getObjectives().get(objKey);
                if (! os.hasPipelineRuns()) {
                    continue;
                }
                for (SamplePipelineRun pipelineRun : os.getPipelineRuns()) {
                    for (PipelineResult result : pipelineRun.getResults()) {
                        if (result instanceof SampleAlignmentResult) {
                            SampleAlignmentResult sar = (SampleAlignmentResult)result;
                            String alignmentSpace = sar.getAlignmentSpace();
                            String imageSize = sar.getImageSize();
                            String opticalResolution = sar.getOpticalResolution();

                            AlignmentContext ctx = new AlignmentContext(alignmentSpace, opticalResolution, imageSize);
                            rtnVal.add(ctx);
                        }
                    }
                }
            }
        }
        return rtnVal;
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
