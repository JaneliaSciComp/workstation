package org.janelia.it.workstation.gui.alignment_board.populator;

import java.util.List;
import javax.swing.JComponent;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.alignment_board.AlignmentBoardContext;
import org.janelia.it.workstation.gui.alignment_board.ab_mgr.AlignmentBoardMgr;
import org.janelia.it.workstation.gui.alignment_board_viewer.AlignmentBoardPanel;
import org.janelia.it.workstation.gui.alignment_board_viewer.LayersPanel;

import org.janelia.it.workstation.nb_action.DropAcceptor;
import org.openide.util.lookup.ServiceProvider;

/**
 * This service provider allows other modules to add things to the running
 * alignment board.
 * 
 * @author fosterl
 */
//@ServiceProvider(service=DropAcceptor.class, path=DropAcceptor.LOOKUP_PATH)
public class AlignmentBoardPopulator implements DropAcceptor {

    /**
     * Accept drops for the alignment board.
     * @param domainObjects list of things bound for board.
     * @param objective specific instance of applicable sample.
     */
    @Override
    public void drop(List<DomainObject> domainObjects, String objective) {
        try {
            AlignmentBoardContext ctx
                    = AlignmentBoardMgr.getInstance().getLayersPanel().getAlignmentBoardContext();
            addEntitiesToAlignmentBoard(ctx, domainObjects, objective);
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
    protected void addEntitiesToAlignmentBoard(
            AlignmentBoardContext alignmentBoardContext, 
            List<DomainObject> domainObjects,
            String objective) throws Exception {
        
        for(DomainObject domainObject : domainObjects) {
            alignmentBoardContext.addDomainObject(domainObject, objective);
        }

    }    

}
