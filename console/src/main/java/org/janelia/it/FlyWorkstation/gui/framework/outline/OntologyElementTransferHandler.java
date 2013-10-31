package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.datatransfer.Transferable;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.ontology.OntologyElement;
import org.janelia.it.jacs.model.ontology.types.OntologyElementType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Support for dragging ontology elements in the OntologyOutline.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class OntologyElementTransferHandler extends EntityTransferHandler {
		
    private static final Logger log = LoggerFactory.getLogger(OntologyElementTransferHandler.class);
    
	public abstract JComponent getDropTargetComponent();

    @Override
    protected Transferable createTransferable(JComponent sourceComponent) {

        log.debug("OntologyElementTransferHandler.createTransferable");
        
        if (sourceComponent instanceof JTree) {
            return super.createTransferable(sourceComponent);    
        }
        
        throw new IllegalStateException("Unsupported component type for transfer: "+sourceComponent.getClass().getName());
    }

    @Override
    public String getAttribute() {
        return EntityConstants.ATTRIBUTE_ONTOLOGY_ELEMENT;
    }

    @Override
    public int getSourceActions(JComponent sourceComponent) {
        return MOVE;
    }
    
	/**
	 * Test if a transfer of a given entities to the target parent node is allowed.
	 * @param targetNode the new parent for the entities
	 * @param entitiesToAdd list of entities to add to the the new parentv
	 * @return true if transfer is allowed
	 */
    protected boolean allowTransfer(EntityTree entityTree, DefaultMutableTreeNode targetNode, List<RootedEntity> entitiesToAdd) {
		
        OntologyOutline ontologyOutline = (OntologyOutline)entityTree;
        
		// Disallow transfer if target node is not owned by the user
		OntologyElement targetElement = ontologyOutline.getOntologyElement(targetNode);
		if (targetElement==null) {
		    log.debug("Disallow transfer because the target does not have an ontology element");
		    return false;
		}
		
		OntologyElementType type = targetElement.getType();

		if (type==null) {
			log.debug("Disallow transfer because the target does not have an ontology element type");
			return false; 
		}
		
		if (!type.allowsChildren()) {
			log.debug("Disallow transfer because the target does not support children");
			return false; 
		}
		
        for(RootedEntity rootedEntity : entitiesToAdd) {

            OntologyElement element = ontologyOutline.getOntologyElement(rootedEntity.getEntityData());
            if (element==null) {
                log.debug("Disallow transfer because the source does not have an ontology element");
                return false;
            }
            
            if (type instanceof org.janelia.it.jacs.model.ontology.types.Enum) {
                if (!(element.getType() instanceof org.janelia.it.jacs.model.ontology.types.EnumItem)) {
                    log.debug("Disallow transfer because the target is an Enum, but the element is not an EnumItem");
                    return false; 
                }
            }
            else {
                if (element.getType() instanceof org.janelia.it.jacs.model.ontology.types.EnumItem) {
                    log.debug("Disallow transfer because the target is not an Enum, but the element is an EnumItem");
                    return false; 
                }   
            }
            
			// Disallow transfer if the entity is in the ancestor chain
			DefaultMutableTreeNode nextParent = (DefaultMutableTreeNode)targetNode.getParent();
			if (nextParent!=null) {
				nextParent = (DefaultMutableTreeNode)targetNode.getParent();
			}
			while (nextParent != null) {
				OntologyElement ancestor = ontologyOutline.getOntologyElement(nextParent);
				if (Utils.areSameEntity(rootedEntity.getEntity(), ancestor.getEntity())) {
					log.debug("Disallow transfer because entity is an ancestor of target");
					return false;
				}
				nextParent = (DefaultMutableTreeNode) nextParent.getParent();
			}
		}
		
		return true;
	}
    
}