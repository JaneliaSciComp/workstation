package org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.outline.EntityTransferHandler;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.model.domain.AlignmentContext;
import org.janelia.it.FlyWorkstation.model.domain.Sample;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.FlyWorkstation.model.viewer.AlignmentBoardContext;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.Iterator;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 10/17/13
 * Time: 3:19 PM
 *
 * Overriding the transfer handler, to enforce drag-prohibit earlier.
 */
public class AlignmentBoardEntityTransferHandler extends EntityTransferHandler {
    private AlignmentBoardViewer viewer;
    private Logger logger;
    public AlignmentBoardEntityTransferHandler( AlignmentBoardViewer viewer ) {
        this.viewer = viewer;
        logger = LoggerFactory.getLogger( AlignmentBoardEntityTransferHandler.class );
    }
    @Override
    public JComponent getDropTargetComponent() {
        return viewer;
    }
    @Override
    public boolean canImport(TransferHandler.TransferSupport support) {
        boolean rtnVal = super.canImport( support );

        if ( rtnVal ) {
            DataFlavor flavor = null;
            try {
                flavor = getNodesFlavor();
                // Get the target entity.
                Transferable transferable = support.getTransferable();

                AlignmentBoardContext abContext = SessionMgr.getBrowser().getLayersPanel().getAlignmentBoardContext();
                Entity abEntity = abContext.getInternalEntity();
                List<RootedEntity> entities = (List<RootedEntity>)transferable.getTransferData( flavor );
                AlignmentContext standardContext = new AlignmentContext(
                        abEntity.getValueByAttributeName( EntityConstants.ATTRIBUTE_ALIGNMENT_SPACE ),
                        abEntity.getValueByAttributeName( EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION ),
                        abEntity.getValueByAttributeName( EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION )
                );
                int acceptedCount = 0;
                for ( RootedEntity entity: entities ) {
                    if ( abContext.isAcceptedType( entity.getType() ) ) {
                        if ( entity.getType().equals( EntityConstants.TYPE_NEURON_FRAGMENT ) ) {
                            Entity sampleEntity = ModelMgr.getModelMgr().getAncestorWithType(entity.getEntity(), EntityConstants.TYPE_SAMPLE);
                            if (sampleEntity==null) {
                                rtnVal = false;
                            }
                            else {
                                rtnVal = isSampleCompatible( standardContext, new RootedEntity( sampleEntity ) );
                            }
                        }
                        else if ( entity.getType().equals( EntityConstants.TYPE_SAMPLE ) ) {
                            rtnVal = isSampleCompatible(standardContext, entity);
                        }
                        acceptedCount ++;
                    }
                }
                // Case: none of the entities were even testable to be rejected a different way.
                if ( acceptedCount == 0 ) {
                    rtnVal = false;
                }

            } catch ( Exception ex ) {
                rtnVal = false;
                ex.printStackTrace();
                logger.error( "failed to check if can import DnD item(s).");
            }
        }
        return rtnVal;
    }

    private boolean isSampleCompatible(AlignmentContext standardContext, RootedEntity entity) throws Exception {
        boolean rtnVal;
        boolean foundMatch = false;
        Sample wrapper = new Sample( entity );
        List< AlignmentContext> contexts = wrapper.getAvailableAlignmentContexts();
        Iterator<AlignmentContext> contextIterator = contexts.iterator();
        while ( ! foundMatch  ) {
            if ( contextIterator.hasNext() ) {
                AlignmentContext nextContext = contextIterator.next();
                if ( standardContext.equals( nextContext ) ) {
                    foundMatch = true;
                }
            }

        }
        rtnVal = foundMatch;
        return rtnVal;
    }

}
