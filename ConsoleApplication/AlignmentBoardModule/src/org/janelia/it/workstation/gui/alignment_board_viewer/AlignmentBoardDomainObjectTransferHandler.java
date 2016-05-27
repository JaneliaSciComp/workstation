package org.janelia.it.workstation.gui.alignment_board_viewer;

import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.alignment_board.ab_mgr.AlignmentBoardMgr;
import org.janelia.it.workstation.gui.framework.outline.TransferableEntityList;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ReverseReference;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentBoard;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentContext;
import org.janelia.it.jacs.model.domain.sample.Image;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.sample.NeuronSeparation;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.workstation.api.entity_model.management.ModelMgrUtils;
import org.janelia.it.workstation.gui.alignment_board.AlignmentBoardContext;
import org.janelia.it.workstation.gui.alignment_board_viewer.creation.DomainHelper;
import org.janelia.it.workstation.gui.browser.events.selection.DomainObjectSelectionModel;
import org.janelia.it.workstation.gui.browser.flavors.DomainObjectFlavor;
import org.janelia.it.workstation.gui.browser.gui.listview.icongrid.AnnotatedImageButton;
import org.janelia.it.workstation.gui.browser.gui.listview.icongrid.DomainObjectTransferHandler;
import org.janelia.it.workstation.gui.browser.gui.listview.icongrid.ImageModel;
import org.janelia.it.workstation.gui.browser.gui.listview.icongrid.TransferableDomainObjectList;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 10/17/13
 * Time: 3:19 PM
 *
 * Overriding the transfer handler, to enforce drag-prohibit earlier.
 */
public class AlignmentBoardDomainObjectTransferHandler extends DomainObjectTransferHandler {
    private static final int MAX_FRAGMENT_CAPACITY = 20000;
    private static final String CAPACITY_EXCEEDED_FMT =
            "Alignment board %s already contains %d fragments.  Your addition of %d would exceed the maximum of %d.";

    private Logger log;
    public AlignmentBoardDomainObjectTransferHandler(ImageModel<DomainObject,Reference> imageModel, DomainObjectSelectionModel selectionModel) {
        super(imageModel, selectionModel);
        log = LoggerFactory.getLogger( AlignmentBoardDomainObjectTransferHandler.class );
    }

    @Override
    protected Transferable createTransferable(JComponent sourceComponent) {

        log.debug("createTransferable sourceComponent={}", sourceComponent);

        DomainHelper domainHelper = new DomainHelper();
        if (sourceComponent instanceof AnnotatedImageButton) {
            List<DomainObject> domainObjects = domainHelper.selectAndInflateCandidateObjects(getSelectionModel().getSelectedIds());
            return new TransferableDomainObjectList(sourceComponent, domainObjects);
        }
        else {
            log.warn("Unsupported component type for transfer: " + sourceComponent.getClass().getName());
            return null;
        }
    }
    
    @Override
    public boolean canImport(TransferHandler.TransferSupport support) {
        boolean rtnVal = true;//super.canImport( support );
        log.debug("Enter can-import...");

        DomainHelper domainHelper = new DomainHelper();
        if ( rtnVal ) {
            try {
                // Get the target entity.
                Transferable transferable = support.getTransferable();
                final AlignmentBoardMgr alignmentBoardMgr = AlignmentBoardMgr.getInstance();                

                // Need check for alignment context compatibility.
                AlignmentBoardContext abContext = alignmentBoardMgr.getLayersPanel().getAlignmentBoardContext();
                AlignmentBoard alignmentBoard = null; //todo

                TransferableDomainObjectList transferableDomainObjectList = (TransferableDomainObjectList)transferable;
                List<DomainObject> transferData = (List<DomainObject>)transferableDomainObjectList.getTransferData(DomainObjectFlavor.LIST_FLAVOR);
                AlignmentContext standardContext = abContext.getAlignmentContext();

                boolean typeIsFragment;
                boolean typeIsSample;
                boolean typeIsRef;

                Sample sampleDO;

                int acceptedCount = 0;
                int fragmentCount = 0;

                // Flip assumptions: turn this back on if something matches.
                rtnVal = false;
                
                for ( DomainObject domainObject: transferData ) {
                    if ( abContext.isAcceptedType(domainObject)) {
                        typeIsFragment = domainObject.getType().equals(NeuronFragment.class.getSimpleName());
                        typeIsSample = domainObject.getType().equals(Sample.class.getSimpleName());
                        // TODO: what type is our 3d now?
                        typeIsRef = domainObject.getType().equals(Image.class.getSimpleName()) && domainObject.getName().startsWith("Reference");
                        if ( typeIsFragment  ||  typeIsRef ) {
                            sampleDO = domainHelper.getSampleForNeuron((NeuronFragment)domainObject);
                            if ( sampleDO == null ) {
                                rtnVal = false;
                            }
                            else {
                                boolean compatible = isSampleCompatible( standardContext, sampleDO );
                                if ( compatible ) {
                                    fragmentCount++;
                                    rtnVal = true;
                                }
                            }
                        }
                        else if ( typeIsSample ) {
                            boolean compatible = isSampleCompatible(standardContext, domainObject);
                            if ( compatible ) {
                                Sample sample = (Sample) domainObject;
                                ReverseReference fragmentsRRef = domainHelper.getNeuronRRefForSample(sample, standardContext.getImageSize());
                                fragmentCount += fragmentsRRef.getCount();
                                        
                                rtnVal = true;
                            }
                        }
                        acceptedCount ++;
                    }
                }
                // Case: none of the entities were even testable to be rejected a different way. May be redundant.
                if ( acceptedCount == 0 ) {
                    rtnVal = false;
                }
                else {
                    // NOTE: if cap test (FW-2012) becomes ill-advised, it can be removed by commenting/removing
                    // only the call below.
                    rtnVal = checkAvailableCapacity(rtnVal, alignmentBoard, fragmentCount);
                }

            } catch ( Exception ex ) {
                rtnVal = false;
                ex.printStackTrace();
                log.error( "failed to check if can import DnD item(s).");
            }
        }
        log.debug("Exit can-import: {}.", rtnVal);
        return rtnVal;
    }

    //todo
    private boolean checkAvailableCapacity(boolean rtnVal, AlignmentBoard abEntity, int fragmentCount) {
        int remainingCapacity = getRemainingFragmentCapacity( abEntity );

        // Next, let's see whether the additions would put it over the top.
        if ( remainingCapacity < fragmentCount ) {
            // disallow.
            JOptionPane.showMessageDialog(
                    SessionMgr.getMainFrame(),
                    String.format(
                            CAPACITY_EXCEEDED_FMT, abEntity.getName(),
                            (MAX_FRAGMENT_CAPACITY - remainingCapacity),
                            fragmentCount,
                            MAX_FRAGMENT_CAPACITY
                    )
            );
            rtnVal = false;
        }

        log.debug( "Remaining capacity is {}.  Adding {}.", remainingCapacity, fragmentCount );
        return rtnVal;
    }

    //todo
    private int getRemainingFragmentCapacity(DomainObject abEntity) {
        log.debug("Getting remaining capacity...");
        int fragmentCount = 0;
        int sampleCount = 0;
        // Some entities would make it onto the board.  Let's get the remaining capacity of that board.
//        for ( DomainObject container: ModelMgrUtils.getAccessibleChildren(abEntity) ) {
//            // Looking at sample contents, only; ignore the compartment sets.
//            if ( ! container.getName().startsWith( EntityConstants.TYPE_COMPARTMENT_SET ) ) {
//                fragmentCount += ModelMgrUtils.getAccessibleChildren(container).size();
//                sampleCount ++;
//            }
//        }

        return MAX_FRAGMENT_CAPACITY - fragmentCount + sampleCount; // Do not let sample count detract.
    }

    //todo
    private boolean isSampleCompatible(AlignmentContext standardContext, DomainObject entity) throws Exception {
        boolean rtnVal;
        boolean foundMatch = false;
        Sample wrapper = (Sample)entity;
        List< AlignmentContext> contexts = null; //wrapper.getObjectiveSamples();
        Iterator<AlignmentContext> contextIterator = contexts.iterator();

        while ( contextIterator.hasNext() && (! foundMatch) ) {
            AlignmentContext nextContext = contextIterator.next();
            if ( standardContext.equals( nextContext ) ) {
                foundMatch = true;
            }

        }

        rtnVal = foundMatch;
        return rtnVal;
    }

}
