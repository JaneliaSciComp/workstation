package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.IndeterminateProgressMonitor;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.model.domain.AlignmentContext;
import org.janelia.it.FlyWorkstation.model.domain.EntityWrapper;
import org.janelia.it.jacs.model.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Support for dragging entity wrappers from the EntityWrapperOutline and dropping them onto the LayersPanel or the
 * alignment board.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class EntityWrapperTransferHandler extends TransferHandler {

	private static final Logger log = LoggerFactory.getLogger(EntityWrapperTransferHandler.class);
	
	private EntityWrapperOutline entityWrapperOutline;
	private DataFlavor nodesFlavor;
	private DataFlavor[] flavors = new DataFlavor[1];
	
	public EntityWrapperTransferHandler() {
		this.entityWrapperOutline = SessionMgr.getBrowser().getEntityWrapperOutline();
		try {
			String mimeType = DataFlavor.javaJVMLocalObjectMimeType + ";class=\""
					+ EntityWrapper.class.getName() + "\"";
			nodesFlavor = new DataFlavor(mimeType);
			flavors[0] = nodesFlavor;
		} 
		catch (ClassNotFoundException e) {
			SessionMgr.getSessionMgr().handleException(e);
		}
	}
    
    public abstract JComponent getDropTargetComponent();
    
    @Override
    protected Transferable createTransferable(JComponent sourceComponent) {

        List<EntityWrapper> wrapperList = new ArrayList<EntityWrapper>();
        
        if (sourceComponent instanceof JTree) {
            JTree tree = (JTree) sourceComponent;
            TreePath[] paths = tree.getSelectionPaths();
            if (paths == null) return null;
            for(TreePath path : paths) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                wrapperList.add(entityWrapperOutline.getEntityWrapper(node));
            }
        }
        else if (sourceComponent instanceof LayersPanel) {
            log.warn("No transfer handling defined from a LayersPanel");
        }
        else {
            throw new IllegalStateException("Unsupported component type for transfer: "+sourceComponent.getClass().getName());
        }
        
        return new TransferableWrapperList(wrapperList);
    }

    @Override
    public int getSourceActions(JComponent c) {
        return LINK;
    }
	
	@Override
	public boolean canImport(TransferHandler.TransferSupport support) {
	    
		try {
			// Only dealing with drag and drop for now
			if (!support.isDrop()) return false;
			if (!support.isDataFlavorSupported(nodesFlavor)) {
				log.debug("Disallow transfer because data flavor {} is not supported",nodesFlavor.getMimeType());
				return false;
			}

			support.setShowDropLocation(true);
			
			// This may be relevant later if we want to drag within the LayersPanel
//			Component sourceComponent = support.getComponent();
			
			// This may be relevant later if we want to drop directly on the tree nodes
//			DropLocation dropLocation = support.getDropLocation();

            LayersPanel layersPanel = SessionMgr.getBrowser().getLayersPanel();
			JComponent dropTarget = getDropTargetComponent();
            
			if (!(dropTarget instanceof LayersPanel)) {
//                log.info("Invalid drop target: {}",dropTarget);
//	            log.info("  layersPanel: {}",layersPanel);
//                log.info("  entityWrapperOutline: {}",entityWrapperOutline);
	            return false;
			}
			
			Transferable transferable = support.getTransferable();
			List<EntityWrapper> wrappers = (List<EntityWrapper>)transferable.getTransferData(nodesFlavor);
			
			if (wrappers.size()>1) {
			    log.error("Cannot transfer more than one entity at a time");
			    return false;
			}
            
//          if (!(dropLocation instanceof JTree.DropLocation)) return false;
			
			AlignmentContext boardAlignmentContext = layersPanel.getAlignmentBoardContext().getAlignmentContext();

//			for(EntityWrapper wrapper : wrappers) {
//			    
//			    EntityContext context = wrapper.getContext();
//			    if (context==null) {
//			        log.info("EntityContext is null for wrapper: {}",wrapper);
//			        return false;
//			    }
//			    AlignmentContext alignmentContext = context.getAlignmentContext();
//			    if (!boardAlignmentContext.canDisplay(alignmentContext)) {
//			        log.warn("The current alignment board ({}) cannot display data from alignment context {}",boardAlignmentContext,alignmentContext);
//			        return false;
//			    }
//			}
			
			return true;
			
		}
		catch (Exception e) {
			SessionMgr.getSessionMgr().handleException(e);
			return false;
		}
	}

	@Override
	public boolean importData(TransferHandler.TransferSupport support) {

		if (!canImport(support)) return false;

		try {
			// Extract transfer data
			Transferable t = support.getTransferable();
			final List<EntityWrapper> wrappers = (List<EntityWrapper>)t.getTransferData(nodesFlavor);
		
			SimpleWorker worker = new SimpleWorker() {
                @Override
                protected void doStuff() throws Exception {
                    for(EntityWrapper wrapper : wrappers) {
                        addEntityWrapper(wrapper);              
                    }   
                }
                
                @Override
                protected void hadSuccess() {
                    SessionMgr.getBrowser().getLayersPanel().refresh();
                }
                
                @Override
                protected void hadError(Throwable error) {
                    SessionMgr.getSessionMgr().handleException(error);
                }
            };
            worker.setProgressMonitor(new IndeterminateProgressMonitor(SessionMgr.getBrowser(), "Adding aligned entities...", ""));
            worker.execute();
			
		}
		catch (Exception e) {
			SessionMgr.getSessionMgr().handleException(e);
			return false;
		}

		return true;
	}
	
	protected void addEntityWrapper(EntityWrapper wrapper) throws Exception {
	    log.info("add entity wrapper : "+wrapper.getName());
	    LayersPanel layersPanel = SessionMgr.getBrowser().getLayersPanel();
	    layersPanel.addNewAlignedEntity(wrapper);
	}
	
	/**
	 * Add the given entities to the given target parent, both in the entity model, and in the view.
	 * @param targetNode the new parent for the entities
	 * @param entitiesToAdd list of entities to add to the the new parent
	 * @param destIndex child insertion index in the new parent
	 * @throws Exception
	 */
	protected void addEntities(DefaultMutableTreeNode targetNode, List<Entity> entitiesToAdd, int destIndex) throws Exception {

//		Entity parentEntity = entityWrapperOutline.getEntity(targetNode);
//		
//		int i = destIndex;
//		for(Entity entity : entitiesToAdd) {
//			log.debug("will add {} to {}",entity.getName(),parentEntity.getName());
//			i++;
//		}
//		log.debug("updating entity model");
//
//		// First update the entity model
//		
//		List<DefaultMutableTreeNode> toRemove = new ArrayList<DefaultMutableTreeNode>();		
//		List<EntityData> eds = EntityUtils.getOrderedEntityDataWithChildren(parentEntity);
//		
//		int origSize = eds.size();
//		int currIndex = destIndex;		
//		
//		for(Entity entity : entitiesToAdd) {
//			// Add the entity to the new parent, generating the ED
//			EntityData newEd = ModelMgr.getModelMgr().addEntityToParent(parentEntity, entity);
//			log.debug("created new child {}",entity.getName());
//			
//			if (destIndex > origSize) {
//				eds.add(newEd);
//			} 
//			else {
//				eds.add(currIndex++, newEd);
//			}
//
//			// Since entities have a single instance, we can no longer tell when we're reordering by comparing 
//			// instances. We need a new way to implement this!
//			
//			if (MOVE_WHEN_REORDERING) {
//		        Enumeration enumeration = targetNode.children();
//		        while (enumeration.hasMoreElements()) {
//		            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) enumeration.nextElement();
//		            EntityData ed = entityWrapperOutline.getEntityData(childNode);
//					if (ed!=null && ed.getChildEntity()!=null && Utils.areSameEntity(entity, ed.getChildEntity()) && ed.getId()!=newEd.getId()) {
//						toRemove.add(childNode);
//					}
//		        }
//			}
//		}
//
//		log.debug("renumbering children and adding to tree");
//		
//		// Renumber the children, re-add the new ones, and update the tree
//		int index = 0;
//		for (EntityData ed : eds) {
//			log.debug("processing ed order={}",ed.getOrderIndex());
//			if ((ed.getOrderIndex() == null) || (ed.getOrderIndex() != index)) {
//				ed.setOrderIndex(index);
//				
//				// Remember actual entities
//				Entity parent = ed.getParentEntity();
//				Entity child = ed.getChildEntity();
//				
//				// For performance reasons, we have to replace the parent with a fake id-only entity. Otherwise, it 
//				// tries to transfer the entire object graph every time, and it gradually grinds to a halt, since the 
//				// object graph grows at some insane rate.
//				Entity fakeParentEntity = new Entity();
//				fakeParentEntity.setId(parentEntity.getId());
//				ed.setParentEntity(fakeParentEntity);
//
//				Entity fakeChildEntity = new Entity();
//				fakeChildEntity.setId(child.getId());
//				ed.setChildEntity(fakeChildEntity);
//				
//				log.debug("will save ED {} with index={}",ed.getId(),index);
//				EntityData savedEd = ModelMgr.getModelMgr().saveOrUpdateEntityData(ed);
//				log.debug("saved ED {}",savedEd.getId());
//				
//				// Restore actual entities
//				ed.setParentEntity(parent);
//				ed.setChildEntity(child);
//			}
//			index++;
//		}
//		
//		// Remove old eds, if the target is the same
//		if (MOVE_WHEN_REORDERING) {
//			for (DefaultMutableTreeNode childNode : toRemove) {
//	            EntityData ed = entityWrapperOutline.getEntityData(childNode);
//				ModelMgr.getModelMgr().removeEntityData(ed);
//				log.debug("removed old ED {}",ed.getId());
//			}
//		}
	}
	
	/**
	 * List of entities being transferred.  
	 */
	public class TransferableWrapperList implements Transferable {
		
		private List<EntityWrapper> wrappers;

		public TransferableWrapperList(EntityWrapper wrapper) {
			this.wrappers = new ArrayList<EntityWrapper>();
			wrappers.add(wrapper);
		}
		
		public TransferableWrapperList(List<EntityWrapper> wrappers) {
			this.wrappers = wrappers;
		}
		
		@Override
		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
			if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
			return wrappers;
		}

		@Override
		public DataFlavor[] getTransferDataFlavors() {
			return flavors;
		}

		@Override
		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return nodesFlavor.equals(flavor);
		}
	}
}