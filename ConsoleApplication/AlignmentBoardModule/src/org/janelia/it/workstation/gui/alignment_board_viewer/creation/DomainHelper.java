/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.alignment_board_viewer.creation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ReverseReference;
import org.janelia.it.jacs.model.domain.compartments.Compartment;
import org.janelia.it.jacs.model.domain.compartments.CompartmentSet;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentBoard;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentBoardItem;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentBoardReference;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentContext;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.sample.NeuronSeparation;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
//import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SampleAlignmentResult;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.sample.SampleProcessingResult;
import org.janelia.it.jacs.model.domain.support.SampleUtils;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.workstation.gui.alignment_board.util.ABCompartment;
import org.janelia.it.workstation.gui.alignment_board.util.ABCompartmentSet;
import org.janelia.it.workstation.gui.alignment_board.util.ABItem;
import org.janelia.it.workstation.gui.alignment_board.util.ABNeuronFragment;
import org.janelia.it.workstation.gui.alignment_board.util.ABSample;
import org.janelia.it.workstation.gui.browser.api.AccessManager;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This helper class will walk around the domain data, finding various pieces
 * of data required for other operations.
 *
 * @author fosterl
 */
public class DomainHelper {
    public static final String ALIGNMENT_BOARDS_FOLDER = "Alignment Boards";

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
                            for (DomainObject dObj: completeList) {
                                AlignmentContext nextCtx = (AlignmentContext)dObj;
                                if (nextCtx.getAlignmentSpace().equals(alignmentSpace)  &&
                                    nextCtx.getImageSize().equals(imageSize)  &&
                                    nextCtx.getOpticalResolution().equals(opticalResolution)) {
                                    
                                    rtnVal.add(nextCtx);
                                }
                            }
                        }
                    }
                }
            }
        }
        return rtnVal;
    }
    
    /** Must walk up to the separation and back down to find this. */
    public AlignmentContext getNeuronFragmentAlignmentContext(Sample sample, NeuronFragment neuronFragment) {
        NeuronSeparation separation = SampleUtils.getNeuronSeparation(sample, neuronFragment);
        PipelineResult parentResult = separation.getParentResult();
        SampleAlignmentResult sar = null;
        AlignmentContext rtnVal = null;
        if (parentResult instanceof SampleAlignmentResult) {
            sar = (SampleAlignmentResult) separation.getParentResult();
            rtnVal = new AlignmentContext();
            rtnVal.setAlignmentSpace(sar.getAlignmentSpace());
            rtnVal.setImageSize(sar.getImageSize());
            rtnVal.setOpticalResolution(sar.getOpticalResolution());
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
    
    /** Creates a board, and returns its ID. */
    public AlignmentBoard createAlignmentBoard(AlignmentBoard board) throws Exception {
        AlignmentBoard rtnVal = null;
        if (board != null) {
            final DomainModel model = DomainMgr.getDomainMgr().getModel();
            rtnVal = (AlignmentBoard)model.save(board);
            if (rtnVal == null) {
                handleException("Failed to create an alignment board.  Null value returned.");
            }
            // Next step: add this new board appropriately to its parent.
            // Get the parent.
            List<TreeNode> nodes = (List<TreeNode>)model.getDomainObjects(TreeNode.class, ALIGNMENT_BOARDS_FOLDER);
            TreeNode alignmentBoardsFolder = null;
            if (nodes != null  &&  !nodes.isEmpty()) {
                for (TreeNode nextNode: nodes) {
                    if (nextNode.getOwnerKey().equals(AccessManager.getSubjectKey())) {
                        alignmentBoardsFolder = nextNode;
                    }
                }
            }
            else {
                // Must create the folder.
                alignmentBoardsFolder = new TreeNode();
                alignmentBoardsFolder.setName(ALIGNMENT_BOARDS_FOLDER);
                alignmentBoardsFolder = model.create(alignmentBoardsFolder);
            }
            model.addChild(alignmentBoardsFolder, rtnVal);
        }
        return rtnVal;
    }
    
    public AlignmentBoard fetchAlignmentBoard(Long alignmentBoardId) throws Exception {
        return (AlignmentBoard)DomainMgr.getDomainMgr().getModel().getDomainObject(AlignmentBoard.class.getSimpleName(), alignmentBoardId);
    }
    
    public void saveAlignmentBoard(AlignmentBoard alignmentBoard) throws Exception {
        DomainMgr.getDomainMgr().getModel().save(alignmentBoard);
    }
    
    public Sample getSampleForNeuron(NeuronFragment nf) {
        Reference sampleRef = nf.getSample();
        return (Sample) DomainMgr.getDomainMgr().getModel().getDomainObject(sampleRef);
    }
    
    public ReverseReference getNeuronRRefForSample(Sample sample, AlignmentContext context) {  
        String objective = getObjectiveForAlignmentContext(context);
        ReverseReference rtnVal = null;
        for (ObjectiveSample oSample: sample.getObjectiveSamples()) {
            SamplePipelineRun latestRun = oSample.getLatestSuccessfulRun();
            if (latestRun == null) {
                log.info("No latest run for {}.", sample.getName());
                return null;
            }

            Date latestDate = null;
            for (SampleAlignmentResult sar: latestRun.getResultsOfType(SampleAlignmentResult.class)) {
                // Pre-emptive bail: after this, we are _known_ to have the
                // correct objective.
                if (!sar.getObjective().equals(objective)  ||
                    !sar.getOpticalResolution().equals(context.getOpticalResolution())  ||
                    !sar.getAlignmentSpace().equals(context.getAlignmentSpace())  ||
                    !sar.getImageSize().equals(context.getImageSize())) {
                    log.debug("Did not match up all of objective and alignment context {}.", sar.getAlignmentSpace());
                    continue;
                }
                log.info("Found result with target objective.");
                NeuronSeparation nResult = sar.getLatestSeparationResult();
                if (nResult == null) {
                    log.info("No neuron separation for {}.", sample.getName());
                } else {
                    Date sarDate = sar.getCreationDate();
                    if (latestDate == null  ||  sarDate.after(latestDate)) {
                        rtnVal = nResult.getFragmentsReference();
                        latestDate = sarDate;
                        log.debug("Found matching sep result. Returning ref.");
                    }                    
                }
            }
            
        }
        return rtnVal;
    }
    
    public String getObjectiveForAlignmentContext(AlignmentContext context) {
        String alignmentSpace = context.getAlignmentSpace();
        int xPos = alignmentSpace.indexOf('x');
        int digitPos = xPos;
        if (xPos > -1) {
            digitPos = xPos;
            while (digitPos > 0  &&  Character.isDigit(alignmentSpace.charAt(digitPos - 1))) {
                digitPos --;
            }
        }
        if (digitPos > -1) {
            return alignmentSpace.substring(digitPos, xPos + 1);
        }
        else {
            log.warn("Failed to find objective string from alignment context {}.", context.getAlignmentSpace());
            return "";
        }
    }
    
    /**
     * Finds all refs in list which are compatible with an alignment board, and
     * inflates them back into the output list.
     *
     * @param ids list of reference ids to check.
     * @return compatible/inflated set of values.
     */
    public List<DomainObject> selectAndInflateCandidateObjects(List<Reference> ids) {
        List<DomainObject> domainObjects = new ArrayList<>();
        for (Reference id : ids) {
            if (id.getTargetClassName().equals(Sample.class.getSimpleName()) ||
                id.getTargetClassName().equals(NeuronFragment.class.getSimpleName()) ||
                id.getTargetClassName().equals(CompartmentSet.class.getSimpleName()) || 
                id.getTargetClassName().equals(Compartment.class.getSimpleName())) {
                
                domainObjects.add(DomainMgr.getDomainMgr().getModel().getDomainObject(id));
            }
        }
        return domainObjects;
    }

    public ABItem getObjectForItem(AlignmentBoardItem item) {
        AlignmentBoardReference ref = item.getTarget();
        if (ref == null) {
            log.warn("Null reference in item {}", item.getName());
            return null;
        }
        DomainModel domainModel = DomainMgr.getDomainMgr().getModel();
        if (ref == null || ref.getObjectRef() == null) {
            log.error("No object ref, or no target for item " + item.getName());
            return null;
        }
        DomainObject domainObject = domainModel.getDomainObject(ref.getObjectRef());
        if (domainObject == null) {
            return null;
        }
        if (domainObject instanceof CompartmentSet) {
            CompartmentSet cs = (CompartmentSet) domainObject;
            if (ref.getItemId() != null) {
                return new ABCompartment(cs.getCompartment(ref.getItemId()));
            } else {
                return new ABCompartmentSet(cs);
            }
        } else if (domainObject instanceof Sample) {
            return new ABSample((Sample) domainObject);
        } else if (domainObject instanceof NeuronFragment) {
            return new ABNeuronFragment((NeuronFragment) domainObject);
        }
        throw new IllegalStateException("Unrecognized item type: " + domainObject.getType());
    }
    private void handleException(String message) {
        Exception ex = new Exception(message);
        SessionMgr.getSessionMgr().handleException(ex);
    }
      
}
