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
import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.AlignmentBoardViewer;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.LayersPanel;
import org.janelia.it.FlyWorkstation.model.domain.AlignmentContext;
import org.janelia.it.FlyWorkstation.model.domain.EntityWrapper;
import org.janelia.it.FlyWorkstation.shared.workers.IndeterminateProgressMonitor;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
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
            
			if (!(dropTarget instanceof LayersPanel) && !(dropTarget instanceof AlignmentBoardViewer)) {
                log.info("Invalid drop target: {}",dropTarget);
	            log.info("  layersPanel: {}",layersPanel);
                log.info("  entityWrapperOutline: {}",entityWrapperOutline);
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