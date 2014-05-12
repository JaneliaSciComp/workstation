package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.outline.EntityTransferHandler;
import org.janelia.it.FlyWorkstation.gui.framework.outline.TransferableEntityList;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.model.domain.AlignmentContext;
import org.janelia.it.FlyWorkstation.model.domain.EntityWrapperFactory;
import org.janelia.it.FlyWorkstation.model.domain.Sample;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.FlyWorkstation.model.viewer.AlignmentBoardContext;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

import java.awt.datatransfer.Transferable;
import java.util.Iterator;
import java.util.List;
import org.janelia.it.FlyWorkstation.gui.alignment_board.ab_mgr.AlignmentBoardMgr;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 10/17/13
 * Time: 3:19 PM
 *
 * Overriding the transfer handler, to enforce drag-prohibit earlier.
 */
public class AlignmentBoardEntityTransferHandler extends EntityTransferHandler {
    private static final int MAX_FRAGMENT_CAPACITY = 20000;
    private static final String CAPACITY_EXCEEDED_FMT =
            "Alignment board %s already contains %d fragments.  Your addition of %d would exceed the maximum of %d.";

    private JComponent dropTarget;
    private Logger logger;
    public AlignmentBoardEntityTransferHandler( JComponent viewer ) {
        this.dropTarget = viewer;
        logger = LoggerFactory.getLogger( AlignmentBoardEntityTransferHandler.class );
    }
    @Override
    public JComponent getDropTargetComponent() {
        return dropTarget;
    }
    @Override
    public boolean canImport(TransferHandler.TransferSupport support) {
        boolean rtnVal = true;//super.canImport( support );
        logger.debug("Enter can-import...");

        if ( rtnVal ) {
            try {
                // Get the target entity.
                Transferable transferable = support.getTransferable();

                // Need check for alignment context compatibility.
                AlignmentBoardContext abContext = AlignmentBoardMgr.getInstance().getLayersPanel().getAlignmentBoardContext();
                Entity abEntity = abContext.getInternalEntity();
                List<RootedEntity> rootedEntities = (List<RootedEntity>)transferable.getTransferData(TransferableEntityList.getRootedEntityFlavor());
                AlignmentContext standardContext = new AlignmentContext(
                        abEntity.getValueByAttributeName( EntityConstants.ATTRIBUTE_ALIGNMENT_SPACE ),
                        abEntity.getValueByAttributeName( EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION ),
                        abEntity.getValueByAttributeName( EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION )
                );

                boolean typeIsFragment;
                boolean typeIsSample;
                boolean typeIsRef;

                Entity sampleEntity;

                int acceptedCount = 0;
                int fragmentCount = 0;

                // Flip assumptions: turn this back on if something matches.
                rtnVal = false;

                for ( RootedEntity rootedEntity: rootedEntities ) {
                    if ( abContext.isAcceptedType(rootedEntity.getType()) ) {
                        typeIsFragment = rootedEntity.getType().equals(EntityConstants.TYPE_NEURON_FRAGMENT);
                        typeIsSample = rootedEntity.getType().equals(EntityConstants.TYPE_SAMPLE);
                        typeIsRef = rootedEntity.getType().equals(EntityConstants.TYPE_IMAGE_3D) && rootedEntity.getName().startsWith("Reference");
                        if ( typeIsFragment  ||  typeIsRef ) {
                            sampleEntity = ModelMgr.getModelMgr().getAncestorWithType(rootedEntity.getEntity(), EntityConstants.TYPE_SAMPLE);
                            if ( sampleEntity == null ) {
                                rtnVal = false;
                            }
                            else {
                                boolean compatible = isSampleCompatible( standardContext, new RootedEntity( sampleEntity ) );
                                if ( compatible ) {
                                    fragmentCount++;
                                    rtnVal = true;
                                }
                            }
                        }
                        else if ( typeIsSample ) {
                            boolean compatible = isSampleCompatible(standardContext, rootedEntity);
                            if ( compatible ) {
                                Sample sample = (Sample) EntityWrapperFactory.wrap(rootedEntity);
                                sample.loadContextualizedChildren( standardContext );
                                fragmentCount += (sample.getChildren().size());
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
                    rtnVal = checkAvailableCapacity(rtnVal, abEntity, fragmentCount);
                }

            } catch ( Exception ex ) {
                rtnVal = false;
                ex.printStackTrace();
                logger.error( "failed to check if can import DnD item(s).");
            }
        }
        logger.debug("Exit can-import: {}.", rtnVal);
        return rtnVal;
    }

    private boolean checkAvailableCapacity(boolean rtnVal, Entity abEntity, int fragmentCount) {
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

        logger.debug( "Remaining capacity is {}.  Adding {}.", remainingCapacity, fragmentCount );
        return rtnVal;
    }

    private int getRemainingFragmentCapacity(Entity abEntity) {
        logger.debug("Getting remaining capacity...");
        int fragmentCount = 0;
        int sampleCount = 0;
        // Some entities would make it onto the board.  Let's get the remaining capacity of that board.
        for ( Entity container: abEntity.getChildren() ) {
            // Looking at sample contents, only; ignore the compartment sets.
            if ( ! container.getName().startsWith( EntityConstants.TYPE_COMPARTMENT_SET ) ) {
                fragmentCount += container.getChildren().size();
                sampleCount ++;
            }
        }

        return MAX_FRAGMENT_CAPACITY - fragmentCount + sampleCount; // Do not let sample count detract.
    }

    private boolean isSampleCompatible(AlignmentContext standardContext, RootedEntity entity) throws Exception {
        boolean rtnVal;
        boolean foundMatch = false;
        Sample wrapper = new Sample( entity );
        List< AlignmentContext> contexts = wrapper.getAvailableAlignmentContexts();
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
