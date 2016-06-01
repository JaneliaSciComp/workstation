package org.janelia.it.workstation.gui.alignment_board_viewer;

import java.awt.datatransfer.DataFlavor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ReverseReference;
import org.janelia.it.jacs.model.domain.compartments.CompartmentSet;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentBoard;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentBoardItem;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.workstation.gui.alignment_board.ab_mgr.AlignmentBoardMgr;
import org.janelia.it.workstation.gui.alignment_board.AlignmentBoardContext;
import org.janelia.it.workstation.gui.alignment_board_viewer.creation.DomainHelper;
import org.janelia.it.workstation.gui.browser.events.selection.DomainObjectSelectionModel;
import org.janelia.it.workstation.gui.browser.flavors.DomainObjectFlavor;
import org.janelia.it.workstation.gui.browser.gui.listview.icongrid.DomainObjectTransferHandler;
import org.janelia.it.workstation.gui.browser.gui.listview.icongrid.ImageModel;
import org.janelia.it.workstation.gui.browser.gui.listview.icongrid.TransferableDomainObjectList;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.nb_action.DropAcceptor;
import org.janelia.it.workstation.nb_action.ServiceAcceptorHelper;

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
    public boolean importData(TransferHandler.TransferSupport support) {
        boolean rtnVal = true;
        log.info("Import data has been called.");
        
        Transferable transferable = support.getTransferable();
        try {
            DomainHelper domainHelper = new DomainHelper();
            List<DomainObject> transferData = (List<DomainObject>) transferable.getTransferData(DomainObjectFlavor.LIST_FLAVOR);
            log.info("Got transfer data {}.", transferData.size());
            
            // Find drop acceptors, and figure out which are compatible.
            ServiceAcceptorHelper saHelper = new ServiceAcceptorHelper();            
            Collection<DropAcceptor> targets = saHelper.findHandler(support.getComponent(), DropAcceptor.class, DropAcceptor.LOOKUP_PATH);
            final AlignmentBoardMgr alignmentBoardMgr = AlignmentBoardMgr.getInstance();                
            AlignmentBoardContext abContext = alignmentBoardMgr.getLayersPanel().getAlignmentBoardContext();
            //AlignmentBoard alignmentBoard = abContext.getAlignmentBoard();
            String objective = domainHelper.getObjectiveForAlignmentContext(abContext.getAlignmentContext());

            for (DropAcceptor acceptor : targets) {
                acceptor.drop(transferData, objective);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            rtnVal = false;
        }
        return rtnVal;
    }
    
    @Override
    public int getSourceActions(JComponent sourceComponent) {
        return COPY;
    }
    
    @Override
    protected Transferable createTransferable(JComponent sourceComponent) {
        /*
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
        */
        // Not transfering anything OUT of the alignment board as of now.
        return null;
    }
    
    @Override
    public boolean canImport(TransferHandler.TransferSupport support) {
        boolean rtnVal = true;//super.canImport( support );
        log.debug("Enter can-import...");
        CompatibilityChecker checker = new CompatibilityChecker();
        if ( rtnVal ) {
            try {
                // Get the target entity.
                Transferable transferable = support.getTransferable();

                final AlignmentBoardMgr alignmentBoardMgr = AlignmentBoardMgr.getInstance();
                AlignmentBoardContext abContext = alignmentBoardMgr.getLayersPanel().getAlignmentBoardContext();
                AlignmentBoard alignmentBoard = abContext.getAlignmentBoard();
                List<DomainObject> transferData = (List<DomainObject>) transferable.getTransferData(DomainObjectFlavor.LIST_FLAVOR);
                int compatibleFragmentCount = checker.getCompatibleFragmentCount(abContext, transferData);

                // NOTE: if cap test (FW-2012) becomes ill-advised, it can be removed by commenting/removing
                // only the call below.
                if (compatibleFragmentCount == 0) {
                    rtnVal = false;
                }
                else {
                    rtnVal = checkAvailableCapacity(rtnVal, alignmentBoard, compatibleFragmentCount);
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

}
