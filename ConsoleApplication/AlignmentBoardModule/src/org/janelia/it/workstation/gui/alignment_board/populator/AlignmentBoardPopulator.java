package org.janelia.it.workstation.gui.alignment_board.populator;

import java.util.List;
import javax.swing.JComponent;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.alignment_board.AlignmentBoardContext;
import org.janelia.it.workstation.gui.alignment_board.ab_mgr.AlignmentBoardMgr;
import org.janelia.it.workstation.gui.alignment_board_viewer.AlignmentBoardPanel;
import org.janelia.it.workstation.gui.alignment_board_viewer.LayersPanel;
import org.janelia.it.workstation.gui.alignment_board_viewer.creation.DomainHelper;

import org.janelia.it.workstation.nb_action.DropAcceptor;
import org.openide.util.lookup.ServiceProvider;

/**
 * This service provider allows other modules to add things to the running
 * alignment board.
 * 
 * @author fosterl
 */
@ServiceProvider(service=DropAcceptor.class, path=DropAcceptor.LOOKUP_PATH)
public class AlignmentBoardPopulator implements DropAcceptor {

    private final DomainHelper domainHelper = new DomainHelper();
    
    /**
     * Accept drops for the alignment board.
     * @param domainObjects list of things bound for board. Not used.
     * @param objective specific instance of applicable sample.
     */
    @Override
    public void drop(List<DomainObject> domainObjects, String objective) {
        try {
            AlignmentBoardContext ctx
                    = AlignmentBoardMgr.getInstance().getLayersPanel().getAlignmentBoardContext();
            addToAlignmentBoard(ctx, domainObjects, objective);
        } catch ( Exception ex ) {
            ModelMgr.getModelMgr().handleException( ex );
        }
    }

    /**
     * Accept drops bound for alignment board or Layers panel.
     * 
     * @param object what to check
     * @return true if compatible type.
     */
    @Override
    public boolean isCompatible(JComponent object) {
        return object instanceof AlignmentBoardPanel ||
               object instanceof LayersPanel;
    }
    
    /**
     * Add the given entities to the specified alignment board, if possible.
     * 
     * @param alignmentBoardContext this receives the entities.
     * @param domainObjects these are added to that alignment board.
     */
    protected void addToAlignmentBoard(
            AlignmentBoardContext alignmentBoardContext, 
            List<DomainObject> domainObjects,
            String objective) throws Exception {
        
        //  Want to make sure we do not lose any of the user's work, that has
        //  taken place prior to dragging/dropping into the board.
        domainHelper.saveAlignmentBoardAsync(alignmentBoardContext.getAlignmentBoard());
        
        //  New add the object(s).
        alignmentBoardContext.addDomainObjects(domainObjects);

    }    

}
