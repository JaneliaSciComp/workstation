package org.janelia.it.workstation.gui.alignment_board_viewer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

import java.awt.datatransfer.Transferable;
import java.util.Iterator;
import java.util.List;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ReverseReference;
import org.janelia.it.jacs.model.domain.compartments.CompartmentSet;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentBoard;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentBoardItem;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentContext;
import org.janelia.it.jacs.model.domain.sample.Image;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.workstation.gui.alignment_board.ab_mgr.AlignmentBoardMgr;
import org.janelia.it.workstation.gui.alignment_board.AlignmentBoardContext;
import org.janelia.it.workstation.gui.alignment_board_viewer.creation.DomainHelper;
import org.janelia.it.workstation.gui.browser.events.selection.DomainObjectSelectionModel;
import org.janelia.it.workstation.gui.browser.flavors.DomainObjectFlavor;
import org.janelia.it.workstation.gui.browser.gui.listview.icongrid.AnnotatedImageButton;
import org.janelia.it.workstation.gui.browser.gui.listview.icongrid.DomainObjectTransferHandler;
import org.janelia.it.workstation.gui.browser.gui.listview.icongrid.ImageModel;
import org.janelia.it.workstation.gui.browser.gui.listview.icongrid.TransferableDomainObjectList;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;

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
                AlignmentBoard alignmentBoard = abContext.getAlignmentBoard();
                List<DomainObject> transferData = (List<DomainObject>)transferable.getTransferData(DomainObjectFlavor.LIST_FLAVOR);
                AlignmentContext standardContext = abContext.getAlignmentContext();

                boolean typeIsFragment;
                boolean typeIsSample;
                boolean typeIsRef;

                Sample sample;

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
                            sample = domainHelper.getSampleForNeuron((NeuronFragment)domainObject);
                            if ( sample == null ) {
                                rtnVal = false;
                            }
                            else {
                                boolean compatible = isSampleCompatible( standardContext, sample );
                                if ( compatible ) {
                                    fragmentCount++;
                                    rtnVal = true;
                                }
                            }
                        }
                        else if ( typeIsSample ) {
                            sample = (Sample)domainObject;
                            boolean compatible = isSampleCompatible(standardContext, sample);
                            if ( compatible ) {
                                ReverseReference fragmentsRRef = domainHelper.getNeuronRRefForSample(sample, domainHelper.getObjectiveForAlignmentContext(standardContext));
                                if (fragmentsRRef != null) {
                                    fragmentCount += fragmentsRRef.getCount();
                                    rtnVal = true;
                                    log.info("Sample {} is compatible.", sample.getName());
                                }
                            }
                        }
                    }
                }
                // Nothing acceptable.
                if ( fragmentCount == 0 ) {
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
        log.info("Exit can-import: {}.", rtnVal);
        return rtnVal;
    }

    private boolean checkAvailableCapacity(boolean rtnVal, AlignmentBoard alignmentBoard, int fragmentCount) {
        int remainingCapacity = getRemainingFragmentCapacity( alignmentBoard );

        // Next, let's see whether the additions would put it over the top.
        if ( remainingCapacity < fragmentCount ) {
            // disallow.
            JOptionPane.showMessageDialog(
                    SessionMgr.getMainFrame(),
                    String.format(
                            CAPACITY_EXCEEDED_FMT, alignmentBoard.getName(),
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

    /** Subtract number of items currently visible, from maximum capacity. */
    private int getRemainingFragmentCapacity(AlignmentBoard alignmentBoard) {
        log.debug("Getting remaining capacity...");
        int visibleItemCount = 0;
        // Some entities would make it onto the board.  Let's get the remaining capacity of that board.
        for (AlignmentBoardItem item: alignmentBoard.getChildren()) {
            if (! item.getTarget().getTargetClassName().equals(CompartmentSet.class.getSimpleName())  &&
                ! item.getTarget().getTargetClassName().equals(Sample.class.getSimpleName())) {
                visibleItemCount ++;
            }
        }

        return MAX_FRAGMENT_CAPACITY - visibleItemCount;
    }

    /** Check if this sample has a context compatible with the 'one of momentum'. */
    private boolean isSampleCompatible(AlignmentContext standardContext, Sample sample) throws Exception {
        boolean rtnVal;
        boolean foundMatch = false;
        DomainHelper domainHelper = new DomainHelper();
        List< AlignmentContext> contexts = domainHelper.getAvailableAlignmentContexts(sample);
        if (contexts.isEmpty()) {
            log.warn("No available contexts in sample {}.", sample.getName());
        }
        Iterator<AlignmentContext> contextIterator = contexts.iterator();

        while ( contextIterator.hasNext() && (! foundMatch) ) {
            AlignmentContext nextContext = contextIterator.next();
            if ( standardContext.getImageSize().equals( nextContext.getImageSize() )  &&
                 standardContext.getAlignmentSpace().equals( nextContext.getAlignmentSpace() )  &&
                 standardContext.getOpticalResolution().equals( nextContext.getOpticalResolution() )) {

                foundMatch = true;
            }

        }

        rtnVal = foundMatch;
        return rtnVal;
    }

}
